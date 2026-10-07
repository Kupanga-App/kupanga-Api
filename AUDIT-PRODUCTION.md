# Audit de mise en production — Kupanga

> **Date** : 6 octobre 2026
> **Périmètre** : back-end `kupanga-Api` (Spring Boot 3.2.2 / Java 21, 208 classes), client WebSocket et gestion du token du front `kupanga-front` (Angular)
> **Objectif** : savoir si l'application peut accueillir de vrais clients, et si elle répond aux usages du **Congo (RDC et Brazzaville)** et de l'**Europe**

---

## 0. Verdict

**L'application ne doit pas être ouverte à de vrais clients dans son état actuel.**

Le code est propre et bien découpé : modules séparés, DTO, specifications, Flyway, PDF, notifications temps réel, back-office. La base est saine. En revanche, la **couche de sécurité n'est pas finie**. Plusieurs failles permettent à un inconnu de **prendre le contrôle de n'importe quel compte**, de **lire des données personnelles** et de **créer des contrats sur le bien d'un autre**. Elles se corrigent vite (estimation : 3 à 5 jours), mais elles bloquent la mise en production.

Sur le plan fonctionnel, l'application est aujourd'hui **pensée pour la France** : loi de 1989, €, DPE, chauffage, code postal obligatoire, identité par e-mail. Pour le Congo, il manque des briques essentielles : **numéro de téléphone, Mobile Money, multi-devise (USD/CDF/XAF), adresses sans code postal, SMS/WhatsApp, et un modèle de bail adapté au droit congolais**.

| Domaine | État | Note |
|---|---|---|
| Architecture / qualité du code | Bonne | 7/10 |
| Authentification | Failles critiques | 3/10 |
| Autorisations (qui a le droit de faire quoi) | Quasi absentes | 2/10 |
| WebSocket temps réel | Fonctionne, pas sécurisé ni robuste | 5/10 |
| Données personnelles / RGPD | Fuites | 3/10 |
| Prêt pour le déploiement (infra) | Partiel | 5/10 |
| Adapté à l'Europe (France) | Bon | 7/10 |
| Adapté au Congo | Insuffisant | 3/10 |

Compilation : `mvnw compile` **OK**. Il existe 54 classes de test, mais je ne les ai pas lancées : elles ont besoin de la base PostGIS de test (`db-test`, port 5434).

---

## 1. Failles critiques (P0, à corriger avant toute mise en ligne)

### P0-1. Toute l'API est publique
`config/SecurityConfig.java:97`
```java
.requestMatchers("/**").permitAll()
//.anyRequest().authenticated()
```
Aucune route n'exige d'être authentifié, et le projet ne contient **aucun `@PreAuthorize`**. La protection repose uniquement sur le fait que certains services appellent `auth.getName()` puis échouent sur l'utilisateur `anonymousUser`. Ce n'est pas un contrôle d'accès, c'est un effet de bord. Une requête sans token renvoie une erreur 404 ou 500 au lieu d'une 401, et toute route qui ne lit pas l'utilisateur est ouverte à tout le monde.

**Correctif** :
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/login", "/auth/register", "/auth/google", "/auth/refresh",
                     "/auth/forgot-password", "/auth/reset-password", "/auth/logout").permitAll()
    .requestMatchers(HttpMethod.GET, "/biens/*").permitAll()
    .requestMatchers(HttpMethod.POST, "/biens/search").permitAll()
    .requestMatchers("/contrats/signer/**", "/etats-des-lieux/signer/**").permitAll()
    .requestMatchers("/ws/**", "/actuator/health").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN") // ou désactivé en prod
    .anyRequest().authenticated())
.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
```
Ajouter ensuite `@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")` sur les actions réservées aux propriétaires.

### P0-2. « Mot de passe oublié » renvoie le token de réinitialisation dans la réponse HTTP
`authentification/service/impl/AuthServiceImpl.java:160` : `return passwordResetToken.getToken();`

Il suffit d'appeler `POST /auth/forgot-password?email=victime@x.com` pour recevoir le token, puis `POST /auth/reset-password?token=...&newPassword=...` pour **prendre le compte de n'importe qui** sans accès à sa boîte mail.

**Correctif** : renvoyer un message générique (« Si un compte existe, un e-mail a été envoyé »), même quand l'e-mail n'existe pas, ce qui évite aussi l'énumération des comptes. Passer `token` et `newPassword` dans le **body JSON**, pas en query string : sinon ils apparaissent dans les logs du proxy et de Sentry. Valider la robustesse du nouveau mot de passe et **révoquer les refresh tokens** après un reset.

### P0-3. Le refresh token met le hash du mot de passe dans le JWT
`AuthServiceImpl.java:110-113`
```java
jwtUtils.generateAccessToken(refreshToken.getUser().getMail(),
                             refreshToken.getUser().getPassword()); // ← ici
```
Le second paramètre est le **rôle**. Après chaque refresh, donc toutes les 5 minutes, le claim `role` contient **le hash BCrypt du mot de passe**. N'importe qui peut lire un JWT en base64, et le hash peut alors être attaqué hors ligne. En plus, le front perd le rôle de l'utilisateur après un refresh.

**Correctif** : `String.valueOf(refreshToken.getUser().getRole())`.

### P0-4. `/auth/me` renvoie le hash du mot de passe
`user/dto/readDTO/UserDTO.java:12` contient un champ `password`, et `UserMapper.toDTO` le remplit. L'exemple Swagger le montre même. **Supprimer le champ `password` de `UserDTO`.**

### P0-5. N'importe quel propriétaire peut agir sur le bien d'un autre (IDOR)
Aucune de ces méthodes ne vérifie que `bien.proprietaire == utilisateur connecté` :

| Méthode | Fichier | Conséquence |
|---|---|---|
| `affectLocataire` | `BienServiceImpl.java:303` | Assigner un locataire au bien d'autrui, qui voit alors le dashboard de ce bien |
| `creerContrat` | `ContratServiceImpl.java:43` | Créer un bail sur n'importe quel bien, sans contrôle de rôle |
| `creerQuittance` | `QuittanceServiceImpl.java:48` | Émettre des quittances pour un bien qui n'est pas le sien |
| `creerEtatDesLieux` | `EtatDesLieuxServiceImpl.java:46` | Même problème |

Aucune de ces méthodes ne vérifie non plus que `emailLocataire` correspond au locataire réel du bien, ni que le `contratId` d'une quittance appartient au même bien.

**Correctif** : créer une méthode commune `bienService.verifierProprietaire(bienId, email)` et l'appeler dans chacune.

### P0-6. Des données personnelles publiques via la recherche de biens
`BienMapper.mapProprietairePublic` (`immobilier/mapper/BienMapper.java:33`) n'ignore que `password`. `GET /biens/{id}` et `POST /biens/search` sont publics et renvoient donc **le nom, l'e-mail et le rôle du propriétaire ET du locataire**. Un robot peut récupérer tous les e-mails de la plateforme et savoir qui habite où.

**Correctif** : un `ProprietairePublicDTO` (prénom + initiale du nom + photo) et **aucune information sur le locataire** dans la vue publique.

### P0-7. Contrats, états des lieux et quittances stockés dans des buckets MinIO publics
`minio/service/impl/MinioServiceImpl.java:89` appelle `createBucketIfNotExists(bucketName, true)` pour **tous** les PDF. Ces documents contiennent noms, adresses, montants et **signatures manuscrites**, et ils sont accessibles à vie par toute personne qui a l'URL. Les URL circulent dans les e-mails, les logs et l'historique du navigateur. Au regard du RGPD, c'est une fuite de données.

**Correctif** : buckets privés pour `contrat-de-bail`, `bucket-etats-des-lieux` et `bucket-des-quittances`. Stocker la **clé de l'objet** en base, pas l'URL. Servir les fichiers par un endpoint authentifié qui génère une **URL présignée de 5 minutes** (`getPresignedObjectUrl`). Pour la pièce jointe Brevo, envoyer le PDF en base64 au lieu d'une URL publique.

### P0-8. Secrets dans l'historique git
Le fichier `.env.dev` a été commité dans `d39d993` (« Ajout CI/CD »), puis supprimé dans `c08769e`. Il reste lisible dans l'historique. Je n'ai pas affiché son contenu (lecture bloquée par une règle de sécurité de l'environnement). **Considère comme compromis tous les secrets qu'il contenait** : clé JWT, mot de passe PostgreSQL, clés MinIO, clé Brevo, Google client secret, mot de passe admin. Fais-les tous **tourner**, puis purge l'historique (`git filter-repo`) si le dépôt est ou a été public.

---

## 2. Authentification : constats détaillés

| # | Constat | Fichier | Gravité |
|---|---|---|---|
| A1 | Inscription **sans `@Valid`** : les règles du mot de passe (8 caractères, majuscule, chiffre) et le format de l'e-mail ne sont jamais vérifiés | `AuthController.java:96` | Haute |
| A2 | Énumération des comptes : le login renvoie 404 « Aucun utilisateur pour l'email X » ou 401 mauvais mot de passe. Il faut un 401 générique | `UserServiceImpl.getUserByEmail` + `AuthServiceImpl.login` | Haute |
| A3 | **Aucune limite de tentatives** (login, forgot-password, register, google). Le brute force et le spam d'e-mails Brevo sont illimités. Ajouter Bucket4j ou une limite au niveau du reverse proxy | — | Haute |
| A4 | Google : `email_verified` n'est pas vérifié avant de **lier un compte existant par e-mail**. Il faut vérifier `payload.getEmailVerified()` | `GoogleTokenVerifierImpl.java` + `AuthServiceImpl.loginWithGoogle` | Haute |
| A5 | **Un seul refresh token par utilisateur** (`@OneToOne`, `user_id UNIQUE`) : se connecter sur le téléphone **déconnecte l'ordinateur**. Gênant pour tout le monde, et surtout pour un propriétaire de la diaspora qui gère depuis plusieurs appareils | `RefreshToken.java`, `V10` | Moyenne |
| A6 | Refresh token sans rotation, stocké en clair. Le stocker sous forme de hash SHA-256 et le faire tourner à chaque `/refresh` | `RefreshTokenServiceImpl` | Moyenne |
| A7 | `resetPassword` lève une `RuntimeException`, qui donne une erreur 500. Le `GlobalExceptionHandler` n'a pas de gestionnaire générique (`Exception`, `AccessDeniedException`, `MaxUploadSizeExceededException`, `IllegalStateException`, `IllegalArgumentException`, `MissingRequestCookieException`) | `AuthServiceImpl.java:171`, `GlobalExceptionHandler` | Moyenne |
| A8 | `/auth/refresh` sans cookie déclenche `MissingRequestCookieException`, qui donne une erreur 500 au lieu d'une 401 | `AuthController.refresh` | Moyenne |
| A9 | `logout` lève une 401 si le token n'existe plus, et n'efface pas le cookie si le token est absent | `AuthServiceImpl.logout` | Faible |
| A10 | La comparaison des e-mails est sensible à la casse (`Jean@x.com` ≠ `jean@x.com`). Normaliser en minuscules à l'inscription et au login | `UserRepository.findByMail` | Moyenne |
| A11 | CORS `allowedOriginPatterns("*")` avec `allowCredentials(true)` : n'importe quel site peut appeler `/auth/refresh` avec le cookie de la victime (SameSite=None en prod) et récupérer un access token | `SecurityConfig.java:121` | Haute |
| A12 | Back-office : mot de passe admin en clair dans une variable d'environnement, comparé avec `equals` (pas en temps constant), sans 2FA ni limite de tentatives | `AdminCredentialsAuthProvider.java:28` | Moyenne |
| A13 | Front : access token dans `localStorage` (exposé en cas de XSS). Acceptable avec une durée de vie de 5 min, mais ajouter une CSP stricte | `kupanga-front/.../auth.service.ts` | Faible |
| A14 | Pas de vérification de l'e-mail à l'inscription : on peut créer un compte avec l'e-mail de quelqu'un d'autre | — | Moyenne |

**Ce qui est bien fait** : BCrypt, access token court (5 min), refresh token en cookie `HttpOnly` + `Secure` + `SameSite` configurable, vérification Google côté serveur avec l'audience, rôle `ADMIN` interdit à l'inscription, deux chaînes de sécurité séparées (API JWT sans état / back-office par session + CSRF).

---

## 3. WebSocket temps réel : constats détaillés

L'architecture est correcte : STOMP + SockJS, authentification JWT au `CONNECT` via `JwtChannelInterceptor`, files privées `/user/queue/messages`, `/user/queue/notifications` et `/user/queue/app-notifications`, persistance avant le push, reconnexion automatique côté front avec un refresh avant chaque connexion. **Le flux de base fonctionne.** Voici ce qui doit changer pour la production :

| # | Constat | Fichier | Gravité |
|---|---|---|---|
| W1 | **N'importe quel utilisateur peut écrire à n'importe qui** sur n'importe quel `bienId`. Rien ne vérifie que l'expéditeur est le propriétaire du bien, ou que le destinataire en est le propriétaire. Porte ouverte au spam et au harcèlement. Règle à appliquer : un des deux participants doit être le propriétaire du bien | `MessageServiceImpl.envoyerMessage` | Haute |
| W2 | Pas de `@Valid` sur `MessagePayload`, et aucune limite de longueur. Un `contenu` null provoque une `NullPointerException` sur `substring`. Un message de 10 Mo est accepté. Ajouter `@Size(max = 4000)` et une `@MessageExceptionHandler` qui renvoie l'erreur sur `/user/queue/errors` (aujourd'hui le client ne reçoit aucun retour) | `MessageController.java:41`, `MessagePayload` | Haute |
| W3 | Les `SUBSCRIBE` ne sont pas contrôlés. Un client peut s'abonner à `/topic/**` ou directement à `/queue/messages-user{sessionId}`. Dans l'intercepteur, n'autoriser que les destinations `/user/queue/**` | `JwtChannelInterceptor` | Moyenne |
| W4 | **Aucun heartbeat côté serveur.** Le simple broker n'envoie pas de battements sans `TaskScheduler`, donc la négociation donne `0,0`. Sur les réseaux mobiles (et surtout au Congo, voir §7), les proxys et NAT coupent les connexions inactives au bout de 30 à 60 s sans que le client s'en aperçoive. Correctif : `enableSimpleBroker(...).setHeartbeatValue(new long[]{10000,10000}).setTaskScheduler(scheduler)` | `WebSocketConfig.java:36` | Haute |
| W5 | Origines WebSocket `*` : à restreindre (la propriété `spring.websocket.allowed-origins` existe dans le yml mais elle n'est jamais lue) | `WebSocketConfig.java:48` | Haute |
| W6 | `marquerConversationLue` ignore le bien : ouvrir la conversation sur le bien A marque aussi comme lus les messages du même expéditeur sur le bien B. Filtrer par `conversationId` | `MessageRepository.java:26` | Moyenne |
| W7 | Le JWT n'est vérifié qu'au `CONNECT`. Une session continue après l'expiration du token, ou après un bannissement ou un changement de mot de passe. Acceptable au départ, mais il faut prévoir une fermeture des sessions à la révocation | `JwtChannelInterceptor` | Faible |
| W8 | Un utilisateur Google sans rôle provoque une `NullPointerException` sur `role.name()`, donc un refus de connexion avec un message trompeur | `JwtChannelInterceptor.java:66` | Faible |
| W9 | **Simple broker en mémoire** : impossible de lancer 2 instances, les messages poussés depuis l'instance A ne parviennent pas aux clients connectés sur B. Pour monter en charge : relais STOMP (RabbitMQ) ou Redis pub/sub. Une instance suffit au lancement, mais c'est à documenter | `WebSocketConfig` | Info |
| W10 | Le contenu des messages est écrit dans les logs `INFO` (50 premiers caractères), et donc envoyé à Sentry. C'est une donnée privée : passer en `DEBUG` sans contenu | `MessageServiceImpl` | Moyenne |
| W11 | Pas d'accusé de réception ni de rattrapage : un message envoyé pendant que le client se reconnecte n'arrive qu'au prochain `GET /historique`. Le front doit recharger l'historique ou les messages non lus dans `onConnect` | front `chat-websocket.service.ts` | Moyenne |
| W12 | Le front s'abonne à la fois à `/user/{email}/queue/...` (ne reçoit jamais rien) et à `/user/queue/...` : supprimer les abonnements avec l'e-mail | front `chat-websocket.service.ts` | Faible |
| W13 | Front : `sendMessage` ignore silencieusement l'envoi si le socket est déconnecté, et le message est perdu. Il faut une file d'attente locale avec un statut « en attente » ou « échec », ou un repli sur un `POST` REST | front | Moyenne |

---

## 4. Autres bugs fonctionnels

| # | Bug | Fichier | Impact |
|---|---|---|---|
| B1 | **Un locataire ne voit jamais ses quittances** : `getQuittancesParLocataire` appelle `findByProprietaireId(locataire.getId())` | `QuittanceServiceImpl.java:175` | `/quittances/mes-quittances` est toujours vide |
| B2 | **Le cache de géocodage a pour clé `ville:codePostal`** : tous les biens d'une même ville ou d'un même code postal reçoivent **les mêmes coordonnées GPS**. Résultats faux sur la carte et pour les POI. La clé doit inclure l'adresse | `GeocodingService.java:50` | Carte fausse |
| B3 | **Uploads limités à 1 Mo** (valeur par défaut de Spring, aucune config `spring.servlet.multipart`). Une photo de smartphone (3 à 8 Mo) échoue avec une erreur 500. La doc Swagger annonce « 10 Mo » | `application.yml` | La création de bien échoue sur mobile |
| B4 | **Création de bien sans `@Valid`** : toutes les contraintes de `BienFormDTO` (regex, min/max, `@NoUrl`) sont ignorées | `BienController.java:156` | Données non validées |
| B5 | Pas de contrôle du type ni de la taille des fichiers uploadés (on peut envoyer un `.exe` ou un `.html` dans un bucket public, donc héberger du contenu malveillant sur ton domaine MinIO). Le nom de fichier d'origine est conservé tel quel | `MinioServiceImpl.uploadImage` | Sécurité |
| B6 | Signature : rien n'empêche de **re-signer côté propriétaire** un contrat ou un EDL déjà `SIGNE` (le statut revient en arrière). `signerLocataire` (contrat) ne vérifie pas le statut. La taille de `signatureBase64` n'a pas de maximum | `ContratServiceImpl.java:95`, `EtatDesLieuxServiceImpl.java:94` | Intégrité des baux |
| B7 | `getContratParToken` lève une `IllegalStateException`, qui donne une erreur 500 | `ContratServiceImpl` | UX |
| B8 | `TypeElement.valueOf(...)` / `EtatElement.valueOf(...)` / `TypeCompteur.valueOf(...)` avec une valeur inconnue donnent une erreur 500 | `EtatDesLieuxServiceImpl` | UX |
| B9 | POI : `retry(10)` × timeout de 20 s × 4 types, soit jusqu'à environ 15 min de threads bloqués par bien, et des requêtes qui saturent Overpass. Par ailleurs, `@Async` reçoit une entité `Bien` détachée | `PoiSearchService.java:86` | Pool de threads saturé |
| B10 | Si le géocodage échoue (Nominatim est faible en Afrique centrale), **la création du bien est refusée**. Voir §7 | `BienServiceImpl.createBien` | Bloquant au Congo |
| B11 | Envois d'e-mails `@Async` lancés **à l'intérieur de la transaction** : l'e-mail part même si la transaction est ensuite annulée. Il y a aussi un risque de `LazyInitializationException` sur les entités passées au thread. Utiliser `@TransactionalEventListener(phase = AFTER_COMMIT)` | `EmailServiceImpl`, services immobilier | Fiabilité |
| B12 | `User` a `cascade = ALL` sur biens et messages : supprimer un utilisateur **supprime ses biens, ses contrats, et les messages de ses interlocuteurs**. Dangereux pour l'obligation de conservation des documents | `User.java`, `Bien.java` | Perte de données |
| B13 | Les montants sont en `Double` : erreurs d'arrondi sur les totaux. Utiliser `BigDecimal` + `NUMERIC(12,2)` | Entités / DTO | Comptabilité |
| B14 | `LocalDateTime` partout, sans fuseau horaire. Problème pour une app utilisée à Paris, Kinshasa (UTC+1) et Lubumbashi (UTC+2). Utiliser `Instant`/`OffsetDateTime` côté back et faire la conversion côté front | Global | Dates fausses selon le pays |

---

## 5. Déploiement et infrastructure

| # | Constat | Action |
|---|---|---|
| D1 | `docker-compose-dev.yml` publie `8085:8080`, alors que l'app écoute sur **8089** | Corriger le mapping (pour la prod, créer un `docker-compose-prod.yml` dédié) |
| D2 | Dockerfile : le conteneur tourne en root, sans options de mémoire JVM (512 Mo de limite dans le compose, risque d'OOM-kill), sans `HEALTHCHECK` | `USER app`, `-XX:MaxRAMPercentage=75`, `HEALTHCHECK` sur `/actuator/health` |
| D3 | Actuator : `show-details: always` et `prometheus`/`metrics` **publics** (voir P0-1) | `show-details: when-authorized`, Prometheus réservé au réseau interne |
| D4 | Swagger exposé en prod | `springdoc.api-docs.enabled=false` en prod, ou accès protégé |
| D5 | Sentry : `send-default-pii: true` + `traces-sample-rate: 1.0` | PII à `false` (RGPD), échantillonnage à 0.1–0.2 (coût) |
| D6 | Flyway `validate-on-migrate: false` en prod | À réactiver une fois les checksums stabilisés |
| D7 | `minio/minio:latest`, `redis/redisinsight:latest` | Figer les versions |
| D8 | Pas de **sauvegarde** PostgreSQL ni MinIO documentée | Prévoir `pg_dump` quotidien + réplication MinIO hors site. Indispensable : ce sont des baux |
| D9 | Spring Boot 3.2.2 (fin du support OSS dépassée), jjwt 0.11.5, springdoc 2.2.0, flying-saucer 9.1.22 | Passer à Spring Boot 3.4/3.5 et lancer `mvn versions:display-dependency-updates` + OWASP dependency-check |
| D10 | Pas de pagination sur `getHistorique`, `getNonLues`, `findByBienId` | À ajouter avant que les volumes grossissent |
| D11 | Pas de journal d'audit (qui a signé quoi, depuis quelle IP, quand). Nécessaire pour la valeur probante des signatures | Table `audit_log` |
| D12 | Pas de suppression ni d'export du compte (RGPD art. 15/17) | Endpoints `DELETE /users/me` (anonymisation) et `GET /users/me/export` |
| D13 | Pas de CGU, de politique de confidentialité ni de bannière cookies côté front | Obligatoire en Europe |

---

## 6. Adéquation au marché européen (France)

**Points forts déjà présents** : bail conforme à la loi n° 89-462 du 6 juillet 1989, quittance (art. 21), état des lieux détaillé (pièces, éléments, compteurs, clés), DPE/GES, signature en ligne, géolocalisation + POI, messagerie.

**Manques pour un vrai client européen** :
1. **Paiement du loyer** : prélèvement SEPA (GoCardless, Stripe SEPA Direct Debit) avec génération automatique de la quittance quand le paiement est reçu.
2. **Révision annuelle du loyer (IRL)** et régularisation des charges.
3. **Signature électronique** : la signature dessinée est une signature « simple » au sens d'eIDAS. Elle est recevable, mais facile à contester. Il faut au minimum un journal d'audit (IP, horodatage, hash du PDF, OTP par e-mail ou SMS au moment de signer). Pour une valeur plus forte : Yousign, DocuSign ou Universign.
4. **Dossier locataire** (pièces justificatives, garant, Visale) : prévoir des documents privés avec chiffrement.
5. **Types de bail** : meublé/vide existe déjà, mais il manque le bail mobilité, étudiant et colocation (clause de solidarité).
6. **Multi-pays UE** : la Belgique, la Suisse et le Luxembourg ont d'autres règles. Isoler les modèles de bail par juridiction (voir §8).
7. **RGPD** : voir P0-6, P0-7, D5, D12, D13. Le registre des traitements et la durée de conservation des documents sont à définir.

---

## 7. Adéquation au contexte congolais (RDC et Congo-Brazzaville)

Voici ce qui bloquera concrètement un propriétaire ou un locataire à Kinshasa ou à Brazzaville aujourd'hui.

### 7.1 Bloquants

| # | Problème | Pourquoi c'est bloquant au Congo | Solution |
|---|---|---|---|
| C1 | **Code postal obligatoire** (`BienFormDTO.codePostal` `@NotBlank`, 3 à 10 caractères) | Les codes postaux ne sont **pas utilisés** au quotidien, ni en RDC ni au Congo-Brazzaville | Rendre `codePostal` optionnel et ajouter **commune / quartier / avenue / numéro de parcelle / point de repère** (« derrière l'église X, en face de la station Y ») |
| C2 | **Géocodage obligatoire, sinon refus** | Nominatim ne trouve pas la plupart des adresses de Kinshasa ou Brazzaville | Rendre le géocodage optionnel, permettre de **placer un repère sur la carte** ou d'utiliser le **GPS du téléphone** sur place, et prendre le centre de la commune comme valeur de repli |
| C3 | **Regex d'adresse** `^[\p{L}0-9 ,.'\-]+$` | Refuse `N°`, `/`, `#`, alors qu'on écrit couramment « N° 12, Av. Kasa-Vubu, Q/Matonge, C/Kalamu » | Élargir la regex (`°`, `/`, `#`, `&`). À faire en même temps que B4 : une fois `@Valid` ajouté, cette regex bloquerait réellement les adresses congolaises |
| C4 | **Monnaie en € figée** dans les PDF et les e-mails | En RDC, les loyers sont surtout fixés en **USD**, parfois en **CDF**. À Brazzaville, en **XAF (FCFA)** | Champ `devise` (ISO 4217 : EUR, USD, CDF, XAF) sur `Bien`, `Contrat` et `Quittance`, et formatage localisé (`NumberFormat.getCurrencyInstance`) |
| C5 | **Loyer plafonné à 100 000** (`@DecimalMax`) | Un loyer de 300 USD vaut environ 850 000 CDF, et une villa à Brazzaville environ 500 000 XAF. Le plafond bloque ces saisies | Plafond qui dépend de la devise, ou pas de plafond métier |
| C6 | **Identité = e-mail uniquement**, pas de champ téléphone dans `User` | Au Congo, **le numéro de téléphone est l'identifiant principal**. Beaucoup de locataires n'ont pas d'e-mail actif, ou ne le consultent pas | Ajouter `telephone` (format E.164, +243 / +242 / +33), **connexion par OTP SMS ou WhatsApp**, et un e-mail facultatif |
| C7 | **Notifications par e-mail uniquement** (invitations à signer, quittances) | Les e-mails sont peu lus. **WhatsApp et SMS** sont les canaux qui fonctionnent | Envoyer les liens de signature et les rappels de loyer par **WhatsApp Business API** et SMS (Africa's Talking, Twilio, Orange SMS API), avec l'e-mail en second canal |
| C8 | **Aucun paiement intégré** : la quittance est marquée « payée » à la main | Au Congo, le loyer se paie surtout en **espèces** et par **Mobile Money** : M-Pesa (Vodacom), Orange Money, Airtel Money, Afrimoney en RDC ; MTN MoMo et Airtel Money à Brazzaville | Intégrer un agrégateur : **CinetPay, FlexPay (RDC), PawaPay, MaxiCash**. Garder la saisie « payé en espèces » avec **photo du reçu** et confirmation du locataire |
| C9 | **Bail fondé sur la loi française de 1989** | Ce modèle n'a pas de valeur au Congo. En RDC, la loi applicable est la **loi n° 15/025 du 31 décembre 2015 relative aux baux à loyer non professionnels** (qui encadre notamment la garantie locative). Le Congo-Brazzaville a sa propre législation | Créer un **modèle de bail par pays** (voir §8), **à faire valider par un juriste local** |

### 7.2 Fonctionnalités à adapter

| # | Aujourd'hui | Ce qu'il faudrait au Congo |
|---|---|---|
| C10 | `TypeBien` : APPARTEMENT, MAISON, STUDIO | + **Villa, Parcelle, Annexe, Chambre, Local commercial, Entrepôt, Terrain** |
| C11 | `ModeChauffage`, `ClasseEnergie`, `ClasseGes` | Inutiles au Congo : à afficher seulement pour la juridiction UE. Ajouter **Eau** (REGIDESO/SNDE, forage, citerne), **Électricité** (SNEL/E2C, **groupe électrogène**, **panneaux solaires**, nombre d'heures de courant par jour), **Clôture/mur**, **Gardiennage**, **Parking**, **Climatisation** |
| C12 | Dépôt de garantie unique | **Garantie locative** (en mois de loyer) **+ loyers payés d'avance** (pratique courante de plusieurs mois d'avance), en deux champs distincts |
| C13 | Compteurs EDL (eau/gaz/électricité) | Ajouter les **compteurs prépayés** (Cash Power) et le relevé du groupe électrogène |
| C14 | POI : école, hôpital, pharmacie, crèche | + **Marché**, **arrêt de taxi/bus**, **église**, **banque/agence Mobile Money**, **station-service**. Les données OSM restent pauvres : permettre la saisie manuelle par le propriétaire |
| C15 | Rôles : PROPRIÉTAIRE / LOCATAIRE | + **GESTIONNAIRE / AGENT** (commissionnaire). **C'est le cas d'usage clé pour la diaspora** : un propriétaire en Europe confie son bien à Kinshasa à un mandataire sur place, qui encaisse, fait l'état des lieux, envoie des photos et des reçus |
| C16 | Signature dessinée + lien e-mail valable 72 h | Lien envoyé par **WhatsApp/SMS** avec un **OTP**, et possibilité d'une signature « en présentiel » sur le téléphone du propriétaire ou de l'agent |

### 7.3 Contraintes réseau et terminaux (Afrique centrale)

| # | Contrainte | Recommandation |
|---|---|---|
| R1 | 3G/4G instable, données mobiles chères | **Compresser et redimensionner les images** à l'upload (vignettes de 400 px + version de 1600 px en WebP), lazy-loading, pagination stricte, compression gzip/brotli côté serveur (`server.compression.enabled=true`) |
| R2 | Coupures réseau fréquentes | **Heartbeat WebSocket (W4)**, file d'attente d'envoi locale (W13), **PWA** (cache hors ligne du dashboard, des quittances et de l'historique) |
| R3 | Smartphones Android d'entrée de gamme | Budget JS du front à surveiller, PDF légers |
| R4 | Latence vers le serveur | Si le serveur est en Europe (env. 120 à 200 ms depuis Kinshasa), c'est acceptable. Prévoir un **CDN** (Cloudflare, présent à Kinshasa) pour les images et les fichiers statiques |
| R5 | Langues | Français (OK pour les deux pays). Plus tard : **Lingala**, Swahili, Kikongo, et anglais pour la diaspora. Externaliser les textes back (e-mails, PDF) dans des `messages_xx.properties` |

### 7.4 Données personnelles côté Congo
La RDC a adopté un **Code du numérique (ordonnance-loi n° 23/010 du 13 mars 2023)** qui contient des dispositions sur la protection des données. En hébergeant en Europe, le RGPD s'applique de toute façon à tous les utilisateurs. Se conformer au RGPD couvre donc l'essentiel, sous réserve d'une vérification juridique locale.

---

## 8. Architecture cible « Congo + Europe » dans une seule application

Plutôt que de dupliquer l'application, introduire une notion de **juridiction** :

```
Bien.pays (ISO 3166 : FR, BE, CD, CG…)
  └─► Juridiction (config)
        ├─ devises autorisées       (FR: EUR │ CD: USD, CDF │ CG: XAF)
        ├─ champs obligatoires       (FR: codePostal, DPE │ CD: commune, quartier)
        ├─ modèle de bail Thymeleaf  (contrat-fr.html │ contrat-cd.html │ contrat-cg.html)
        ├─ modèle de quittance / EDL
        ├─ règles (plafond de garantie, durée minimale, préavis)
        ├─ moyens de paiement        (FR: SEPA, CB │ CD/CG: Mobile Money, espèces)
        └─ canaux de notification    (FR: e-mail │ CD/CG: WhatsApp > SMS > e-mail)
```

- `Contrat`, `Quittance` : ajouter `devise`, `juridiction`, `modeleVersion` (pour savoir quel modèle a servi à générer un PDF déjà signé).
- `User` : `telephone`, `telephoneVerifie`, `pays`, `langue`, `fuseauHoraire`.
- Un `PaymentProvider` (interface) avec des implémentations `StripeSepaProvider`, `CinetPayProvider`, `FlexPayProvider`, `CashManualProvider`, et un webhook qui génère automatiquement la quittance.
- Un `NotificationChannel` (interface) : `EmailChannel` (Brevo), `SmsChannel`, `WhatsAppChannel`, avec un ordre de préférence par juridiction et par utilisateur.

---

## 9. Plan d'action proposé

### Sprint 1 (bloquant, environ 1 semaine) : sécurité
- [ ] P0-1 `authorizeHttpRequests` + `@PreAuthorize` + entry point 401
- [ ] P0-2 forgot/reset password (ne plus renvoyer le token, body JSON, révocation)
- [ ] P0-3 rôle dans le refresh
- [ ] P0-4 retirer `password` de `UserDTO`
- [ ] P0-5 contrôle de propriété sur bien, contrat, quittance, EDL
- [ ] P0-6 DTO public sans e-mail et sans locataire
- [ ] P0-7 buckets privés + URL présignées
- [ ] P0-8 faire tourner tous les secrets, purger l'historique
- [ ] A1/B4/W2 `@Valid` partout, A11/W5 CORS restreint
- [ ] A3 limites de tentatives (login, forgot, register)
- [ ] Gestionnaire d'exceptions générique (fini les erreurs 500 avec stacktrace)
- [ ] Tests d'intégration de sécurité : un test par endpoint, qui vérifie la 401 sans token et la 403 sur la ressource d'un autre utilisateur

### Sprint 2 (environ 1 semaine) : fiabilité
- [ ] B1 quittances locataire, B2 clé de cache du géocodage, B3 multipart à 10 Mo, B5 validation des fichiers
- [ ] W1 règles de messagerie, W3 contrôle des SUBSCRIBE, W4 heartbeat, W6 lecture par conversation, W11/W13 côté front
- [ ] B6 machine à états des signatures, B9 POI (1 retry, timeout de 10 s, cache)
- [ ] B11 e-mails `AFTER_COMMIT`, B12 cascades
- [ ] D1–D8 Docker, actuator, Sentry, sauvegardes, versions figées

### Sprint 3–4 : ouverture au Congo (MVP Kinshasa)
- [ ] C1/C2/C3 adresses congolaises + repère sur la carte + géocodage optionnel
- [ ] C4/C5/B13 multi-devise + `BigDecimal`
- [ ] C6/C7 téléphone + OTP + notifications WhatsApp/SMS
- [ ] C8 Mobile Money (un agrégateur) + paiement en espèces avec reçu photo
- [ ] C9 modèle de bail RDC validé par un juriste
- [ ] C10–C13 types de biens, équipements, garantie + avance
- [ ] R1 compression des images, R2 PWA

### Ensuite
- [ ] C15 rôle Gestionnaire/Agent (diaspora)
- [ ] Paiement SEPA (Europe), révision IRL
- [ ] Journal d'audit + OTP de signature (valeur probante)
- [ ] RGPD : export et suppression du compte, CGU, politique de confidentialité
- [ ] Mise à jour de Spring Boot et des dépendances, broker externe si plusieurs instances

---

## 10. Ce que cet audit ne couvre pas
- Tests unitaires et d'intégration **non exécutés** : il faut la base `db-test`. Lance `docker compose --profile test up -d db-test && ./mvnw verify`.
- Front-end : seuls le service WebSocket et le service d'authentification ont été lus, pas l'ensemble des écrans (accessibilité, responsive, XSS dans les templates).
- Pas de test d'intrusion dynamique (les failles ci-dessus viennent d'une lecture du code).
- Le contenu de `.env.dev` dans l'historique git n'a pas été lu.
- Les références juridiques congolaises sont données comme **pistes** et doivent être validées par un juriste local avant de rédiger les modèles de bail.
