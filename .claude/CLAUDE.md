# CLAUDE.md — Mémoire partagée du projet Kupanga API

> Ce fichier est la **mémoire de projet** de Claude Code, partagée dans le repo.
> **Règle pour Claude** : à chaque décision prise ou modification apportée au projet, mettre à jour ce fichier
> (sections *Avancement*, *Décisions* et *Journal*) dans la même tâche, sans attendre qu'on le demande.
> Langue de travail : **français**.

---

## 1. Objectif actuel

Rendre Kupanga **déployable en production pour tout type de client**, en **Europe (France)** et au **Congo (RDC + Brazzaville)**.
Référence : [`AUDIT-PRODUCTION.md`](../AUDIT-PRODUCTION.md) (audit du 2026-10-06). Verdict : **pas prêt** — failles de sécurité P0 bloquantes, puis adaptation Congo.

## 2. Le projet en bref

- Plateforme de gestion locative propriétaire / locataire : biens, contrats de bail (signature en ligne), états des lieux, quittances PDF, messagerie temps réel, notifications, back-office admin.
- **Stack** : Spring Boot 3.2.2 · Java 21 · PostgreSQL + PostGIS · Flyway (dernière migration : `V34`) · Redis (cache) · MinIO (fichiers/PDF) · Brevo (e-mails) · JWT (jjwt 0.11.5) + Google OAuth · STOMP/SockJS · Thymeleaf + Flying Saucer (PDF) · MapStruct · Lombok · Sentry · springdoc.
- **Packages** (`com.kupanga.api.*`) : `authentification`, `backoffice`, `chat`, `config`, `email`, `exception`, `immobilier`, `minio`, `notification`, `pagination`, `user`.
- **Port** : `8089` (`PORT`). Profils : `dev` (défaut, lit `.env.dev`), `prod`, `test`.
- **Sécurité** : 2 chaînes — `BackOfficeSecurityConfig` `@Order(1)` (`/backoffice/**`, session + formLogin, admin via `ADMIN_EMAIL`/`ADMIN_PASSWORD`) et `SecurityConfig` `@Order(2)` (API REST JWT stateless).
- **Front** : repo séparé `kupanga-front` (Angular 19, Tailwind, Signals + Services, standalone components).
- **CI** : `.github/workflows/ci.yml` (PostGIS + Flyway + MinIO, build Maven, tests).
- Autres docs (dossier `docs/`, **ignoré par git**) : `docs/DAT.md` (archi back), `docs/DAT-front.md`, `docs/retex.md`, `docs/dossier-projet.md`. `README.md` et `AUDIT-PRODUCTION.md` restent à la racine.
- Ce fichier est **`.claude/CLAUDE.md`** (chargé automatiquement par Claude Code au démarrage ; ne pas le déplacer dans `docs/`).

## 3. Commandes utiles

```bash
./mvnw compile                                         # compilation
docker compose -f docker-compose-dev.yml --profile test up -d db-test   # base PostGIS de test (port 5434)
./mvnw verify                                          # tests (nécessitent db-test)
```

## 4. Conventions

- Toute feature back-office reste dans `com.kupanga.api.backoffice.*` (Thymeleaf + Bootstrap 5, palette Night `#10212B` / Olive `#8FA464` / Mint `#EFFBDB`, fonts DM Serif Display / DM Sans / DM Mono, icônes Lucide).
- Toute évolution de schéma = nouvelle migration Flyway `V{n+1}__description.sql` (ne jamais modifier une migration existante).
- Ne jamais commiter de secrets (`.env.dev` est dans `.gitignore`).
- **Claude ne committe jamais et ne pousse jamais** (`git commit` / `git push` interdits, bloqués dans `.claude/settings.local.json`). L'utilisateur fait tous les commits ; Claude propose un message de commit court, en français.

## 4ter. Outillage Claude Code

- **Skills par sprint** (`.claude/skills/`) : `/sprint-1-securite`, `/sprint-2-fiabilite`, `/sprint-3-juridiction`, `/sprint-4-congo`. Argument : un ID de tâche (ex. `/sprint-1-securite P0-3`), `tout`, ou rien (= première case non cochée).
- **Déroulé commun** : `.claude/sprint-workflow.md` (comprendre → implémenter → tests → agent `testeur` → agent `revue-securite` → mise à jour de `.claude/CLAUDE.md` → rapport + message de commit suggéré).
- **Agents** (`.claude/agents/`) : `testeur` (lance Maven, résume les échecs, Sonnet) et `revue-securite` (relecture en lecture seule à la lumière de l'audit). Aucun des deux ne modifie de fichier.
- Prérequis tests : Docker démarré + `docker compose -f docker-compose-dev.yml --profile test up -d db-test`.

## 4bis. Architecture multi-juridiction (validée le 2026-10-07)

Le **pays du bien** sélectionne un **profil de juridiction** ; le code lit ce profil, jamais de `if (pays == ...)` dispersés.

- **`Pays`** : enum ISO 3166 (`FR`, `BE`, `CD`, `CG`) remplace le texte libre `Bien.pays` (migration Flyway de conversion).
- **Données → YAML** : `kupanga.juridictions.<PAYS>` lu par `JuridictionProperties` (`@ConfigurationProperties`) : `locale`, `fuseau`, `devises`, `devise-defaut`, `champs-obligatoires`, `champs-masques`, `types-bien`, `modele-documents`, `notification`. Table en BDD seulement si on veut éditer depuis le back-office.
- **Comportement → stratégie** : interface `RegleJuridiction` (`validerBien`, `validerContrat`…) + une implémentation par pays (`RegleFrance`, `RegleRdc`…), résolue par `JuridictionRegistry.pour(pays)`.
- **Validation** : annotations universelles sur `BienFormDTO` ; champs dépendant du pays (`codePostal`, DPE, plafonds, `commune`, `quartier`, `avenue`, `numeroParcelle`, `pointDeRepere`) facultatifs dans le DTO et contrôlés par une contrainte de classe `@ValideSelonJuridiction`.
- **BDD** : colonnes classiques laissées vides si non utilisées (pas de JSONB fourre-tout).
- **Figé sur les documents** : `Bien.devise` (EUR/USD/CDF/XAF) + montants en `BigDecimal` ; `Contrat` et `Quittance` copient `pays`, `devise`, `modeleVersion` (ex. `cd-v1`) à leur création. Interdit de changer le pays d'un bien qui a des contrats.
- **Modèles PDF** : `templates/documents/<pays>/contrat.html` (+ quittance, EDL), fragments communs partagés ; montants formatés avec `NumberFormat.getCurrencyInstance(locale)`.
- **Front** : `GET /juridictions/{pays}` renvoie la config du formulaire ; Angular construit le formulaire dynamiquement ; le back revalide toujours.
- **Notifications** : FR → e-mail ; CD/CG → e-mail + bouton lien `wa.me`.
- Démarrer avec **FR + CD** ; ajouter CG/BE = surtout de la config + un modèle de bail.

## 5. Avancement du plan de l'audit

Légende : `[ ]` à faire · `[~]` en cours · `[x]` fait (préciser date + commit dans le Journal).

### Sprint 1 — Sécurité (bloquant) · skill `/sprint-1-securite`
- [x] P0-3 rôle (et non hash du mot de passe) dans le JWT au refresh (`AuthServiceImpl.java:110`) (2026-10-07)
- [x] P0-4 retirer `password` de `UserDTO` (2026-10-07)
- [x] P0-2 forgot/reset password : ne plus renvoyer le token, body JSON, message générique (+ A2), révocation des refresh tokens (2026-10-07)
- [x] P0-1 `authorizeHttpRequests` + `@PreAuthorize` + entry point 401 (`SecurityConfig.java:97`) (2026-10-07)
- [x] P0-5 contrôle de propriété (IDOR) : `affectLocataire`, `creerContrat`, `creerQuittance`, `creerEtatDesLieux` (2026-10-07)
- [x] P0-6 DTO public propriétaire sans e-mail, aucune info locataire en public (2026-10-07)
- [x] P0-7 buckets MinIO privés pour contrats/EDL/quittances + URL présignées 5 min (2026-10-07)
- [x] VALID `@Valid` partout (A1/B4/W2) + regex d'adresse élargie (C3) + bornes des DTO de recherche + `UserFormDTO` borné + échappement HTML des e-mails (2026-10-08)
- [x] CORS CORS et origines WebSocket restreints (A11/W5) (2026-10-08)
- [x] A3 rate limiting (login, forgot, register, google) (2026-10-08)
- [x] EXC gestionnaire d'exceptions générique, plus de 500 (+ A7/A8) (2026-10-08)
- [x] TESTS-SECU tests d'intégration sécurité (401 sans token, 403 sur ressource d'autrui) (2026-10-08)
- [x] P0-8 **manuel (utilisateur)** : rotation de tous les secrets de `.env.dev` (fuités dans `d39d993`) + purge de l'historique git (2026-10-08, fait par l'utilisateur)
  - Actions : (1) changer tous les secrets fuités dans `d39d993` : clé JWT, mot de passe PostgreSQL, clés MinIO, clé API Brevo, secret Google OAuth, mot de passe admin ; (2) si le dépôt est ou a été public, purger l'historique avec `git filter-repo` (réécriture irréversible, faite par l'utilisateur, jamais par Claude).
- **Sprint 1 terminé (2026-10-08)** : revue de fin de sprint « prêt pour le sprint 2 », 442 tests verts. **POINT DE REPRISE — à faire en tout premier à la prochaine session** : avant toute autre tâche, présenter à l'utilisateur le résumé de la revue de fin de Sprint 1 (§7, bloc « Revue de fin de Sprint 1 » : bloquants de mise en ligne, 3 points IMPORTANTS, mineurs) et lui poser les **2 questions en attente** :
  1. **Back-office** : corriger maintenant le login admin (`AdminCredentialsAuthProvider` : `LimiteurTentatives` + comparaison `MessageDigest.isEqual`), ou le protéger par l'infrastructure (`/backoffice/**` non exposé publiquement) ?
  2. **A4** (`email_verified` Google) : le remonter du Sprint 4 au Sprint 2, à côté de A14, comme le recommande la revue ?
  Puis lancer le Sprint 2 selon ses réponses. Rappeler aussi que tout le Sprint 1 est non commité (message suggéré : « Sprint 1 sécurité : contrôle d'accès, validation, CORS, limite de tentatives, erreurs et tests d'intégration »).

### Sprint 2 — Fiabilité · skill `/sprint-2-fiabilite`
- [ ] B1 quittances locataire
- [ ] A10 e-mails normalisés en minuscules (+ migration, index unique `lower(mail)`)
- [ ] B2 clé du cache de géocodage
- [ ] B3 multipart 10 Mo
- [ ] B5 validation des fichiers uploadés
- [ ] W1 règles de la messagerie
- [ ] W3 contrôle des SUBSCRIBE
- [ ] W4 heartbeat WebSocket
- [ ] W6 lecture par conversation
- [ ] W10 contenu des messages hors des logs
- [ ] W11-13 front : rattrapage, abonnements, file d'envoi (repo `kupanga-front`)
- [ ] B6 machine à états des signatures
- [ ] B7-B8 erreurs 500 sur entrées invalides
- [ ] B9 POI (1 retry, timeout 10 s, cache)
- [ ] B11 e-mails `AFTER_COMMIT`
- [ ] A14 vérification de l'e-mail à l'inscription (lien via Brevo)
- [ ] B12 cascades
- [ ] D1-D8 Docker, actuator, Swagger, Sentry, Flyway, versions figées, sauvegardes

### Sprint 3 — Socle multi-juridiction (cf. §4bis) · skill `/sprint-3-juridiction`
- [ ] J1 enum `Pays` + migration Flyway de conversion de `Bien.pays`
- [ ] J2 `JuridictionProperties` (YAML FR + CD) + `RegleJuridiction` + `JuridictionRegistry`
- [ ] J3 `devise` + `BigDecimal` + copie `pays`/`devise`/`modeleVersion` sur contrat et quittance (couvre C4/C5/B13)
- [ ] J4 champs d'adresse congolais + `@ValideSelonJuridiction` (couvre C1/C3/B4)
- [ ] J5 modèles de documents par pays `templates/documents/<pays>/` (couvre C9, bail RDC à valider par un juriste)
- [ ] J6 endpoint `GET /juridictions/{pays}` + formulaire dynamique Angular

### Sprint 4 — Fonctionnalités Congo (MVP Kinshasa) · skill `/sprint-4-congo`
- [ ] C2 repère sur la carte / GPS du téléphone + géocodage optionnel (B10)
- [ ] C6 champ `telephone` (E.164 : +243 / +242 / +33) dans le profil, **sans vérification** (contact, pas identifiant)
- [ ] C7-C16 liens WhatsApp click-to-chat (`https://wa.me/<numéro>?text=...`) préremplis pour les liens de signature et rappels de loyer, envoyés par le propriétaire depuis son téléphone
- [ ] A4 vérifier `email_verified` sur la connexion Google (canal d'identité gratuit pour les utilisateurs Android)
- ~~Connexion par OTP SMS/WhatsApp~~ → **reporté** (cf. Décisions 2026-10-07)
- ~~C8 Mobile Money (agrégateur)~~ → **abandonné** (aucun paiement via l'app, cf. Décisions 2026-10-07)
- [ ] C8-bis suivi manuel des paiements : le propriétaire déclare le loyer reçu hors app (mode : espèces, Mobile Money, virement…) + photo du reçu optionnelle (stockage MinIO **privé**, cf. P0-7)
- [ ] C10-C13 types de biens, équipements, garantie + avance
- [ ] R1 compression des images
- [ ] R2 PWA (repo `kupanga-front`)

### Ensuite
- [ ] C15 rôle Gestionnaire/Agent (diaspora)
- ~~SEPA~~ → **abandonné** (aucun paiement via l'app)
- [ ] Révision annuelle du loyer IRL (Europe)
- [ ] Journal d'audit + OTP de signature
- [ ] RGPD : export/suppression compte, CGU, politique de confidentialité
- [ ] Montée de version Spring Boot 3.4/3.5 + dépendances ; broker externe si multi-instances

## 6. Décisions prises

| Date | Décision | Raison |
|---|---|---|
| 2026-05-18 | Back-office séparé en Thymeleaf (package `backoffice`, chaîne de sécurité dédiée, pas de JWT ni BDD pour l'admin) | Isoler l'admin de l'API REST |
| — | Front Angular 19 + Tailwind + Signals (pas de NgRx) | Réactivité légère sans overhead |
| 2026-10-07 | Suivre le plan de l'audit `AUDIT-PRODUCTION.md` dans l'ordre des sprints (sécurité d'abord) | Les P0 bloquent toute mise en ligne |
| 2026-10-07 | Cible architecture : une seule app multi-**juridiction** (`Bien.pays` → devises, champs, modèles de bail, canaux de notif) plutôt que dupliquer l'app | Servir Europe + Congo avec un seul code (cf. audit §8) |
| 2026-10-07 | **Aucun paiement ne transite par l'app** (pas de Mobile Money, SEPA, CB, `PaymentProvider` ni webhook). L'app ne fait que générer les quittances et suivre le statut payé/non payé, saisi à la main | Décision produit de l'utilisateur. Écarte l'audit §6.1, C8 et le `PaymentProvider` du §8 |
| 2026-10-07 | **On garde le monolithe Spring Boot** : messagerie et notifications temps réel restent dans l'app (pas de service NestJS séparé). Le jour où il faudra plusieurs instances : relais STOMP (RabbitMQ) ou Redis pub/sub dans Spring (cf. W9) | Les problèmes W1–W13 sont des règles et de la config, pas une limite de Spring. Un service séparé imposerait un partage de données (biens/users pour W1), un bus d'événements pour les notifs et deux stacks à maintenir. À revoir seulement si la charge du chat devient très différente ou si une équipe dédiée s'en occupe |
| 2026-10-07 | **Pas de Keycloak** : on garde l'auth actuelle (JWT maison + refresh token en cookie + Google OAuth) et on la corrige via le sprint 1 (P0-2, P0-3, A1–A14) | Keycloak ne corrige pas les failles métier (P0-5/6/7), ne gère pas l'OTP téléphone en natif (C6) et ajoute un serveur à exploiter. À revoir si des clients pro demandent du SSO, si plusieurs apps doivent partager la connexion, ou si le 2FA devient obligatoire. Alternative à regarder ce jour-là : Firebase Auth (OTP SMS natif) |
| 2026-10-07 | **Pas d'OTP SMS/WhatsApp au MVP Congo**. Identité = e-mail + mot de passe ou Google (inchangé, Brevo gratuit). Le téléphone est un simple champ de contact non vérifié. Notifications Congo via liens WhatsApp `wa.me` préremplis, envoyés manuellement par le propriétaire. OTP ajouté plus tard, limité au reset du mot de passe et à la signature | Le SMS n'a pas d'offre gratuite (frais facturés par message par les opérateurs) et le budget est nul. Les liens `wa.me` ne coûtent rien et le message vient d'un numéro connu du locataire. Les téléphones Android ont tous un compte Google. Remplace la proposition de l'audit C6/C7 « connexion par OTP » |
| 2026-10-07 | **Paramétrage par juridiction** : enum `Pays` ISO → profil YAML (`JuridictionProperties`) pour les données + interface `RegleJuridiction` par pays pour la logique + valeurs figées (`pays`, `devise`, `modeleVersion`) sur contrats/quittances + modèles PDF par pays + config de formulaire exposée au front. Détails en §4bis | Une seule base de code sans `if` dispersés ; ajouter un pays = config + modèle ; un bail signé ne change jamais si la config évolue. Précise la décision « app multi-juridiction » ci-dessus |
| 2026-10-07 | **C8-bis retenu** : suivi manuel des paiements (déclaration par le propriétaire du mode de paiement hors app + photo du reçu optionnelle) | Rend la quittance utile au Congo (espèces, Mobile Money) sans qu'aucun argent ne transite par l'app ; cohérent avec la décision « aucun paiement via l'app » |
| 2026-10-07 | **Claude ne committe jamais** : l'utilisateur fait tous les commits | Exigence de l'utilisateur ; garde la maîtrise de l'historique |
| 2026-10-07 | **Pas de commit par tâche** : Claude enchaîne les tâches sans demander de committer ; l'utilisateur committe tout à la fin (étape 0 de `sprint-workflow.md` adaptée) | Choix de l'utilisateur |
| 2026-10-07 | Travail des sprints via **une skill par sprint** + 2 agents transverses (`testeur`, `revue-securite`), pas un agent par sprint | Les tâches d'un sprint partagent le même code et contexte ; les agents servent de second regard indépendant et isolent les logs Maven |
| 2026-10-07 | **A10 et A14 ajoutés au Sprint 2** (casse des e-mails, vérification de l'e-mail à l'inscription) | Rapides à corriger ; A14 se fait par e-mail Brevo (gratuit), cohérent avec « pas d'OTP SMS ». Restent non planifiés : A5, A6, A9, A12, A13, W7, W8, D9–D13 |
| 2026-10-07 | **Swagger désactivé en prod** (springdoc off dans `application-prod.yml`), public en dev/test | Choix de l'utilisateur pour P0-1 : le plus simple et le plus sûr ; l'admin n'a pas de JWT, le protéger par rôle n'aurait pas de sens dans la chaîne API |
| 2026-10-07 | **Chat : destinataire déduit du bien** quand `emailDestinataire` est absent (premier message depuis une annonce → propriétaire du `bienId`) | P0-6 retire l'e-mail du propriétaire de la vue publique ; choix de l'utilisateur (impact front : envoyer `bienId` + `contenu` sans e-mail) |
| 2026-10-08 | **A3 : rate limiting avec Bucket4j + Redis** (compteurs partagés dans le Redis du cache), seuils : login 5 / 15 min par couple (e-mail, IP) + 30 / 15 min par e-mail + 20 / 15 min par IP ; forgot-password 3 / h par e-mail + 20 / h par IP ; register 5 / h par IP ; google 20 / 15 min par IP ; réponse 429 + `Retry-After` ; fail-open si Redis indisponible | Choix de l'utilisateur : indépendant de l'hébergement (non décidé), limite aussi par e-mail visé (le reverse proxy ne sait faire que par IP), testable en Maven. Clé (e-mail, IP) pour qu'un tiers ne bloque pas la victime ; limite IP sur forgot pour protéger le quota Brevo (choix de l'utilisateur après revue) |
| 2026-10-08 | **Assignation d'un locataire réservée aux candidats du bien** (conversation propriétaire–locataire sur ce bien, cf. `recherche-locataire`), sinon 404 identique à un id inconnu ; un locataire peut louer plusieurs biens | Trou trouvé par TESTS-SECU : un propriétaire pouvait assigner n'importe quel compte à son bien et lire son e-mail. Choix de l'utilisateur |
| 2026-10-07 | `CLAUDE.md` = mémoire partagée du projet, mise à jour automatiquement par Claude | Garder le contexte entre les conversations |

## 7. Points ouverts / questions

- Choix du fournisseur SMS/WhatsApp pour l'OTP : reporté (à étudier quand l'app aura des utilisateurs/revenus ; comparer les tarifs +243/+242).
- Hébergement prod (Europe + CDN Cloudflare ?) : non décidé.
- Photos de profil (`bucket-photo-profil`) toujours en bucket **public** (donnée personnelle RGPD) : à passer en privé + URL présignée (hors P0-7, à planifier). Idem pour les futurs `Document` des biens et le reçu C8-bis (à mettre dans `MinioConstant.BUCKETS_PRIVES`).
- P0-7 en prod : vérifier en recette qu'une URL présignée s'ouvre depuis le navigateur (région `minio.region` = celle du serveur, reverse proxy qui ne réécrit pas l'en-tête Host). Utiliser un compte MinIO de service limité plutôt que root (avec P0-8).
- CSRF de `/auth/refresh` et `/auth/logout` (cookie `SameSite=None` en prod) : aujourd'hui protégés seulement par le refus CORS de l'en-tête `Origin`. Défense en profondeur possible : exiger un en-tête `X-Requested-With` (force un préflight) ou vérifier `Origin`/`Referer` sur ces deux routes (MINEUR, revue CORS).
- **[Action utilisateur — recette] A3 : vérifier que l'app voit l'IP réelle du client.** Procédure (sans code) sur l'app déployée, avec 2 connexions différentes (PC en Wi-Fi + téléphone en 4G, Wi-Fi coupé) :
  1. depuis le PC, 6 connexions avec un mauvais mot de passe sur le même e-mail → 5 × 401 puis **429** ;
  2. aussitôt, depuis le téléphone, 1 connexion sur ce même e-mail → **401 = OK** (IP distinctes) ; **429 = KO** (l'app voit l'IP du proxy pour tout le monde → toute la plateforme partage la limite par IP).
  Si KO : donner à Claude l'hébergeur / proxy (Render, Cloudflare…) pour configurer `server.tomcat.remoteip.internal-proxies` / `trusted-proxies`. Attendre 15 min entre deux essais (fenêtre des compteurs). Contexte : `server.forward-headers-strategy: native` ne fait confiance qu'aux proxys à IP privée.
- **[Action utilisateur — après ouverture au Congo] A3 : surveiller les IP partagées** (NAT opérateur, cybercafés) :
  - logs de l'hébergeur : chercher `Limite de tentatives atteinte : REGISTER_PAR_IP` (ou `LOGIN_PAR_IP`, `FORGOT_PASSWORD_PAR_IP`…) ; ni e-mail ni IP n'y figurent ; des occurrences fréquentes = vrais utilisateurs bloqués ;
  - retours utilisateurs « Trop de tentatives » dès l'arrivée sur le site ;
  - correctif : relever le seuil dans `LimiteTentatives.java` (ex. `REGISTER_PAR_IP` de 5 à 20 / h). Pas urgent tant que l'app n'a pas d'utilisateurs au Congo.
- A3 : Upstash facture à la commande (~2 commandes Redis par seau, ~6 par login) : à prendre en compte dans le choix de l'offre Redis.
- Messages d'exception qui reprennent l'e-mail saisi (`UserNotFoundException`, `UserAlreadyExistsException`) et l'id (`UserServiceImpl:87`) : données du demandeur lui-même, mais permettent de savoir si un compte existe → à traiter avec A14.
- Assignation (W1, sprint 2) : un propriétaire qui connaît l'e-mail d'un locataire peut lui écrire (créant la conversation) puis l'assigner sans son accord → exiger une conversation ouverte par le locataire ou une acceptation. `findConversationWithBienIdAndEmailExpediteur` renvoie un `Optional` : deux conversations pour le même trio (pas de contrainte d'unicité) → 500 ; passer en `existsBy…` + contrainte unique.
- Tokens de signature contrat/EDL invalides → 401 (`ContratServiceImpl:76`, `EtatDesLieuxServiceImpl:139`) : le front risque de rediriger vers la connexion ; préférer 404/410.
- Tests BDD : les `@DataJpaTest` (`*SpecificationTest`) et `SecuriteApiIntegrationTest` utilisent `ddl-auto=create-drop` sur la base partagée (en CI `kupanga_dev`, migrée par Flyway) : le schéma Hibernate remplace celui de Flyway pendant le run, aucun test ne valide les migrations. À revoir avec D1-D8 (base dédiée ou Testcontainers + Flyway).
- **Revue de fin de Sprint 1 (2026-10-08)** — à arbitrer :
  - [IMPORTANT] Login du back-office (`AdminCredentialsAuthProvider:28`) : aucune limite de tentatives et comparaison du mot de passe non constante (`equals`) → `LimiteurTentatives` + `MessageDigest.isEqual`, ou ne pas exposer `/backoffice/**` publiquement (A3/A12).
  - [IMPORTANT] DTO non bornés : `EtatDesLieuxFormDTO` (pas de `@Valid` imbriqué, listes et chaînes sans `@Size`) et `SignatureDTO` (pas de max, route de signature publique) → 500 ou PDF coûteux (avec B6/B8).
  - [IMPORTANT] **Contrat d'API modifié par le Sprint 1 → livrer `kupanga-front` en même temps que l'API** : forgot/reset en body JSON ; `GET /biens/{id}` public réduit (écrans proprio → `GET /users/biens/{id}`) ; parties de contrat sans id/rôle ; 403 au lieu de 401 sur la propriété, 401 au lieu de 404 au login ; 429 sur les routes d'auth ; `size` > 50 → 400 ; `urlPdf` expire après 5 min (pas de cache) ; chat : `bienId` + `contenu` sans `emailDestinataire` ; assignation d'un non-candidat → 404 ; 409 contrat déjà signé, 413 fichier trop gros, 401 refresh sans cookie.
  - [BLOQUANT mise en ligne] **A4 + A14** : `email_verified` Google non vérifié (`AuthServiceImpl:257`) + inscription sans vérification d'e-mail → prise de contrôle anticipée de compte. Recommandation de la revue : remonter A4 du Sprint 4 au Sprint 2, avec A14.
  - Bloquants mise en ligne déjà planifiés : W1, W3, B1, B3, D2, D8, RGPD (D12/D13), recette prod (IP réelle, URL présignée, `CORS_ALLOWED_ORIGINS`, compte MinIO de service).
  - [MINEUR] `Retry-After` non exposé en CORS (`setExposedHeaders`) ; `@Size(max=255)` manquant sur `LoginDTO.email` / `ForgotPasswordDTO.email` ; e-mails dans les logs INFO et stacktraces des exceptions métier en WARN (avec W10) ; timing de `forgotPassword` (énumération freinée par A3) ; Prometheus plus exposé en prod ; `traces-sample-rate: 1.0` en prod (→ 0.1, D5) ; e-mails personnels en exemples Swagger (`EtatDesLieuxController`, `AuthController`) à remplacer par des adresses fictives.
  - Non planifiés : A6 (refresh token en clair, sans rotation), A9, A12, CSRF de `/auth/refresh`/`/auth/logout`.
- **MinIO n'est plus distribué en image Docker officielle** (dépôt communautaire archivé) : la CI et le dev utilisent l'image Chainguard figée par digest (pas de mises à jour de sécurité automatiques). Pour la prod (D1-D8) : choisir entre cette image (digest mis à jour régulièrement), un fork maintenu, ou une alternative S3 (Garage, RustFS, S3 managé).
- Prod : renseigner `CORS_ALLOWED_ORIGINS` si le front n'est pas servi depuis `https://kupanga.lespacelibellule.com`.
- Front `kupanga-front` : vérifier qu'aucune recherche n'envoie `size > 50` (désormais 400 depuis VALID).
- `codePostal` reste obligatoire dans `BienFormDTO` (vrai blocage dès VALID) : à rendre facultatif selon la juridiction en J4.

## 8. Journal des modifications

> Format : `AAAA-MM-JJ — quoi (fichiers / commit)`. Le plus récent en haut.

- 2026-10-08 — CI cassée (`pull access denied for minio/minio` : MinIO a retiré ses images de Docker Hub) : image remplacée par `cgr.dev/chainguard/minio` figée par digest + `--user 0:0` (image non root, sinon `file access denied` sur `/data`) dans `.github/workflows/ci.yml` (2 jobs) et `docker-compose-dev.yml` (`user: "0:0"`) ; démarrage + `/minio/health/live` vérifiés en local — non commité

- 2026-10-08 — **Sprint 1 terminé** : revue de fin de sprint (`git diff 502b82e`) « prêt pour le sprint 2 », `verify` 442 tests VERT (dont `RateLimitRedisTest`) ; points à arbitrer notés en §7 — non commité
- 2026-10-08 — P0-8 : rotation des secrets fuités (`d39d993`) et purge de l'historique si nécessaire, faites par l'utilisateur (confirmé le 2026-10-08) ; checklist en §5
- 2026-10-08 — TESTS-SECU : `SecuriteApiIntegrationTest` (`@SpringBootTest`, 13 tests) : découverte automatique des routes REST (401 de sécurité sans token, routes publiques vérifiées), statut exact sur les ressources d'autrui (bien, contrat, EDL, quittance, notification, créations), rôles croisés, recherches filtrées, vrai JWT (valide / mal signé / expiré), témoins légitimes ; trou corrigé : `affectLocataire` exige une conversation sur le bien (`BienServiceImpl`, `BienServiceImplTest`) ; bug corrigé : bien sans photo introuvable (`BienRepository.findWithAllProperties` : `left join fetch b.images`) ; `verify` 442 tests VERT, revue-securite OK (2e tour) — non commité
- 2026-10-08 — Sentry : `send-default-pii: false` en prod (`application-prod.yml`) : plus de JWT, cookie de refresh, IP ni utilisateur envoyés à Sentry (revue EXC, demandé par l'utilisateur) — non commité
- 2026-10-08 — EXC (A7/A8) : `TechnicalExceptionHandler` (`@RestControllerAdvice` limité aux `@RestController`, priorité la plus basse) : 403 `AccessDeniedException`, 401 `AuthenticationException`/`MissingRequestCookieException` (A8), 413 upload, 400 multipart/type de paramètre/`IllegalArgumentException`, statut conservé pour les `ErrorResponse` 4xx, sinon 500 générique (détail en log seulement) ; `GlobalExceptionHandler` en `HIGHEST_PRECEDENCE` ; B7 : contrat déjà signé → `KupangaBusinessException` 409 (`ContratServiceImpl`) ; message STOMP fixe sur JWT invalide (`JwtChannelInterceptor`) ; A7 déjà traité en P0-2 ; tests `TechnicalExceptionHandlerTest`, WebMvc Bien/Auth ; `verify` 427 tests VERT, revue-securite OK — non commité
- 2026-10-08 — CI : service Redis (`redis:7-alpine`) démarré avant les tests dans `.github/workflows/ci.yml` pour exécuter `RateLimitRedisTest` ; actions de recette A3 (IP réelle, IP partagées) détaillées en §7 — non commité
- 2026-10-08 — A3 : limite de tentatives Bucket4j 8.14 (`bucket4j_jdk17-core`/`-lettuce`) sur Redis (client Lettuce de Spring, clés `kupanga:rate-limit:*` avec expiration), stockage mémoire en profil test (`app.rate-limit.stockage`) ; package `authentification/ratelimit` (`LimiteTentatives`, `LimiteurTentatives`, `RateLimitConfig`, `StockageSeaux`) appelé dans `AuthController` (login, forgot-password, register, google) ; `TropDeTentativesException` → 429 + `Retry-After` (`GlobalExceptionHandler`) ; fail-open avec log limité à 1 / min ; `server.forward-headers-strategy: native` en prod ; tests `LimiteurTentativesTest`, `RateLimitRedisTest` (vrai Redis, ignoré si absent), `AuthControllerWebMvcTest` ; `verify` 419 tests VERT, revue-securite OK (2e tour) — non commité
- 2026-10-08 — CORS (A11/W5) : `CorsProperties` (`app.cors.allowed-origins` / `CORS_ALLOWED_ORIGINS`, refuse liste vide, `*` et format invalide, retire le `/` final) appliqué à `SecurityConfig` (`setAllowedOrigins`) et au endpoint STOMP `/ws` (`WebSocketConfig`) ; propriété morte `spring.websocket.allowed-origins` supprimée ; pagination du back-office bornée (`BienAdminSearchDTO`, `UserAdminSearchDTO` : page ≥ 0, size 1..100) ; tests `CorsPropertiesTest`, `WebSocketConfigTest`, CORS dans `HealthControllerWebMvcTest`/`SecurityConfigTest`/`AuthControllerWebMvcTest`, `AdminSearchDTOTest`, pagination hors bornes → 400 sur les 5 autres recherches ; `verify` 409 tests VERT, revue-securite OK — non commité
- 2026-10-08 — VALID terminé : `@Min(0)` page / `@Min(1) @Max(50)` size / `@Size` chaînes et listes sur les 6 DTO de recherche ; `UserFormDTO` (nom/prénom 50 car. + lettres, mail 255, mot de passe 72) ; valeurs utilisateur échappées (`HtmlUtils.htmlEscape`) dans le HTML des e-mails (`EmailServiceImpl`) ; `MessageController` : toute `BusinessException` renvoyée, `UserNotFoundException` générique ; tests ajoutés (Bien/Auth WebMvc, `EmailServiceImplTest`, `MessagePayloadValidationTest`) ; `verify` 393 tests VERT, revue-securite OK — non commité
- 2026-10-07 — VALID (1re partie) : `@Valid` partout + W2 + C3 — non commité
- 2026-10-07 — P0-7 : buckets contrats/EDL/quittances privés (politique publique retirée au démarrage et à l'upload), clé d'objet stockée (`cle_pdf`, migration `V35`), `urlPdf` des DTO = URL présignée 5 min (`DocumentPdfUrlMapper`, client `minioPresignClient` sur l'URL publique, `minio.region`), PDF joints aux e-mails Brevo en base64 ; `SecurityConfigTest` passé en profil `test` (touchait la base dev) — non commité
- 2026-10-07 — P0-6 : vue publique des biens en `BienPublicDTO` + `ProprietairePublicDTO` (prénom, initiale, photo ; ni locataire ni documents) ; nouvelle route privée `GET /users/biens/{id}` (`BienService.getBienPrive`) ; parties des contrats sans id/rôle ; chat : `emailDestinataire` facultatif → propriétaire du bien — non commité
- 2026-10-07 — P0-5 : `BienService.verifierProprietaire` (404/403) + `verifierLocataireDuBien` (400) appelés dans `affectLocataire` (+ rôle locataire exigé), `creerContrat`, `creerEtatDesLieux`, `creerQuittance` (+ contrat du même bien), `getQuittancesParBien` ; refus de propriété en 403 au lieu de 401 ; `/users/biens` : un locataire ne voit que ses propres contrats/quittances ; dashboard : bail du locataire connecté uniquement — non commité
- 2026-10-07 — P0-1 : API fermée par défaut (`anyRequest().authenticated()` + liste publique + 401 via `HttpStatusEntryPoint`), `/actuator/**` en `denyAll` sauf health, `@PreAuthorize` rôle propriétaire/locataire sur les actions métier, Swagger désactivé en prod, health sans détails en prod, `@Component` retiré de `JwtFilter` (`SecurityConfig`, contrôleurs `immobilier`/`user`, `application-prod.yml`, tests WebMvc) — non commité
- 2026-10-07 — P0-2 + A2 : forgot/reset en body JSON (`ForgotPasswordDTO`, `ResetPasswordDTO` avec robustesse + `@Size`), message générique sans token, 400 générique sur token invalide/expiré, révocation du refresh token après reset (`RefreshTokenService.revokeAllForUser`) ; login : 401 générique « E-mail ou mot de passe incorrect » + BCrypt factice si e-mail inconnu (pas d'énumération par le temps) (`AuthController`, `AuthServiceImpl`, `InvalidPasswordException`, `PasswordResetTokenServiceImpl`) — non commité
- 2026-10-07 — P0-4 : champ `password` retiré de `UserDTO` et des mappers (`UserMapper`, `BienMapper`, `ContratMapper`), hash retiré de l'exemple Swagger de `/auth/me` ; tests `UserMapperTest` + `$.password` absent — non commité
- 2026-10-07 — P0-3 : rôle (et plus le hash du mot de passe) dans le JWT au refresh ; compte Google sans rôle → claim `""` + `requiresRoleSelection=true` ; log de l'entité `User` remplacé par son id (`AuthServiceImpl`, `AuthServiceImplTest`) — non commité
- 2026-10-07 — Décision : pas de commit par tâche, l'utilisateur committe à la fin (`.claude/sprint-workflow.md` étape 0).
- 2026-10-07 — `/docs/` ajouté au `.gitignore` (docs non versionnées, à la demande de l'utilisateur).
- 2026-10-07 — `CLAUDE.md` déplacé de `docs/` vers `.claude/CLAUDE.md` (chargement automatique) ; références des skills, agents et du workflow mises à jour. Docs rangées dans `docs/` par l'utilisateur.
- 2026-10-07 — A10 et A14 ajoutés au Sprint 2 (`CLAUDE.md` §5 + skill `sprint-2-fiabilite`).
- 2026-10-07 — Outillage créé : skills `.claude/skills/sprint-{1-securite,2-fiabilite,3-juridiction,4-congo}`, `.claude/sprint-workflow.md`, agents `.claude/agents/{testeur,revue-securite}.md`, deny `git commit/push` dans `.claude/settings.local.json`. Plan §5 réorganisé (IDs alignés sur les skills, Sprint 3 et 4 séparés).
- 2026-10-07 — Décision : C8-bis (suivi manuel des paiements + reçu photo) confirmé dans le plan.
- 2026-10-07 — Décision : architecture multi-juridiction validée (§4bis ajouté, étapes J1–J6 dans le Sprint 3–4).
- 2026-10-07 — Décision : pas d'OTP SMS au MVP Congo, téléphone en contact + liens wa.me + Google. Plan C6/C7 réécrit.
- 2026-10-07 — Décision : pas de Keycloak, on garde et on corrige l'auth actuelle (retiré des points ouverts).
- 2026-10-07 — Décision : on garde le monolithe (pas de NestJS séparé pour le chat et les notifs). Keycloak ajouté aux points ouverts.
- 2026-10-07 — Décision : aucun paiement via l'app. Plan mis à jour (C8 et SEPA abandonnés, C8-bis « suivi manuel » proposé).
- 2026-10-07 — Création de `CLAUDE.md` à partir de `AUDIT-PRODUCTION.md` et des mémoires existantes (back-office, front Angular).
- 2026-10-06 — Audit de mise en production rédigé (`AUDIT-PRODUCTION.md`).
