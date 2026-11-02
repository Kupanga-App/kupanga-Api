# Audit infrastructure et passage en production — Kupanga

> **Date** : 2 novembre 2026
> **Périmètre** : stockage des fichiers (MinIO), envoi des e-mails (Brevo), hébergement de tout le back-end (`kupanga-Api` et ses services)
> **Objectif** : quitter MinIO pour un stockage S3 auto-hébergeable et maintenu, ne plus perdre d'e-mail important, pouvoir changer de fournisseur SMTP sans toucher au code, et héberger le back-end en entier sur un serveur prêt à recevoir de vrais clients.
> **Suite de** : [`AUDIT-PRODUCTION.md`](AUDIT-PRODUCTION.md) (Sprints 1 à 4 terminés, cf. `.claude/CLAUDE.md` §5).

---

## 0. Verdict

Le code métier est prêt (Sprints 1 à 4, 798 tests verts). **L'infrastructure ne l'est pas** :

| Domaine | État | Risque |
|---|---|---|
| Stockage des fichiers | MinIO (image Chainguard figée, projet open source archivé) sur une VM Oracle Always Free | Plus de correctifs de sécurité ; VM gratuite récupérable par Oracle ; URL complètes de MinIO enregistrées en base |
| E-mails | API HTTP Brevo, envoi unique en mémoire, aucun nouvel essai | **Un e-mail en échec est perdu** (lien de confirmation, reset, invitation à signer, quittance) ; changer de fournisseur = réécrire le client |
| Hébergement | Render gratuit (512 Mo, mise en veille), Neon, Upstash, Oracle | 4 fournisseurs, quotas gratuits, démarrage lent, Redis facturé à la commande, aucune garantie de service |
| Sauvegardes | Procédure écrite (`docs/sauvegardes.md`), rien d'automatisé | Perte de données possible |

Il faut un **Sprint 5 (code)** puis un **Sprint 6 (serveur)**, décrits au §6. Les skills `/sprint-5-infra` et `/sprint-6-deploiement` les déroulent.

---

## 1. Stockage des fichiers : quitter MinIO

### 1.1 Constats

**S1. MinIO n'est plus maintenu en open source.** Le dépôt communautaire a été archivé (avril 2026) et il n'y a plus d'image Docker officielle. Le projet utilise `cgr.dev/chainguard/minio` figée par digest (`docker-compose-dev.yml`, `.github/workflows/ci.yml`) : plus aucun correctif de sécurité automatique.

**S2. Le code dépend du SDK MinIO** (`io.minio:minio:8.5.4`) dans 12 classes. Point d'entrée unique : `MinioService` / `MinioServiceImpl` (bonne base). Le SDK MinIO parle à tout serveur S3, mais il suit le sort du projet MinIO. Le SDK AWS v2 (`software.amazon.awssdk:s3`) est le standard S3 maintenu.

**S3. Les URL complètes sont enregistrées en base.** `BienImage.url`, `BienImage.urlVignette` et `User.urlProfile` contiennent `https://minio-backend.duckdns.org/<bucket>/<objet>` (`MinioServiceImpl.url`). Changer de domaine ou de stockage casse toutes les photos. `estUrlDuBucket` et `supprimerParUrl` reposent sur ce préfixe. Les documents privés, eux, stockent déjà une clé (`cle_pdf`, `cleRecu`) : c'est le bon modèle.

**S4. Politiques de bucket.** `createBucketIfNotExists` appelle `setBucketPolicy` / `deleteBucketPolicy` **à chaque envoi**. Beaucoup de serveurs S3 ne les gèrent pas (Garage, Cloudflare R2) : tous les envois échoueraient. Créer les buckets et leurs droits est une tâche de démarrage ou de déploiement, pas une tâche par requête.

**S5. Noms MinIO dans la config** : `minio.*`, `app.url-mino` (faute de frappe), `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` en prod (compte **root** utilisé par l'application, cf. §7 de `CLAUDE.md`).

**S6. Photos de profil en bucket public** (point ouvert depuis P0-7) et photos d'avant R1 avec EXIF/GPS : la migration est le bon moment pour les traiter.

### 1.2 Comparatif des stockages S3 auto-hébergeables

| | **RustFS** | **Garage** | **SeaweedFS** | MinIO (Chainguard) |
|---|---|---|---|---|
| Licence | Apache 2.0 | AGPL v3 | Apache 2.0 | AGPL v3 (archivé) |
| Maturité | 1.0 GA (sept. 2026), jeune | Stable depuis 2022, petit périmètre | Ancien, très utilisé | Gelé |
| Politiques de bucket (lecture publique) | Oui (compatibles MinIO) | **Non** sur l'API S3 (lecture anonyme seulement via le point d'accès « website ») | Partielles | Oui |
| URL présignées | Oui | Oui | Oui | Oui |
| Mémoire en nœud unique | ~200-500 Mo | ~100-200 Mo | ~300 Mo (plusieurs processus) | ~300 Mo |
| Migration depuis MinIO | La plus simple (mêmes concepts, `mc mirror` / `rclone`) | Changement de modèle pour les buckets publics | Moyenne | — |

**Recommandation : RustFS en nœud unique**, derrière le reverse proxy, **avec un code qui ne dépend d'aucun serveur en particulier** (SDK AWS v2, aucune politique posée par l'application, clés en base). Ainsi, Garage, SeaweedFS ou un S3 managé (Scaleway, OVH, R2) restent un simple changement de configuration. RustFS étant jeune : version figée, sauvegarde hors site des objets (§4) et test de restauration.

### 1.3 Cible

- Package `stockage` (renommage de `minio`) : interface `StockageService` inchangée dans son esprit, implémentation `S3StockageService` (SDK AWS v2, `S3Client` + `S3Presigner`, `pathStyle` configurable, région configurable).
- Config neutre : `kupanga.stockage.{endpoint, url-publique, region, path-style, access-key, secret-key}` ; variables `STOCKAGE_*`. Compte **de service** limité aux buckets de Kupanga (jamais root).
- **Base : uniquement des clés** (`bien_images.cle`, `cle_vignette`, `utilisateurs.cle_photo`) ; l'URL est construite à la lecture (mapper). Migration Flyway qui extrait la clé des URL actuelles.
- Buckets et droits créés **au démarrage** (ou par le script de déploiement), plus jamais à l'envoi. Démarrage qui échoue clairement si un bucket privé est lisible publiquement.
- Photos de biens : bucket public servi par `fichiers.<domaine>` (Cloudflare en cache). Photos de profil : **privées + URL présignée** (règle le point RGPD ouvert).
- Dev, CI et tests sur l'image RustFS figée.

---

## 2. E-mails : ne plus rien perdre, n'importe quel fournisseur SMTP

### 2.1 Constats

**M1. Couplage à Brevo.** `BrevoEmailClient` appelle `https://api.brevo.com/v3/smtp/email` ; le DTO `BrevoEmail` (format Brevo) circule dans `EmailServiceImpl`, `EmailAEnvoyer`, `EmailEnvoiListener` ; config `brevo.*`. Changer de fournisseur demande de réécrire le client.

**M2. Un e-mail en échec est perdu.** `EmailEnvoiListener.envoyer` : un seul essai, l'exception est journalisée (`E-mail non envoyé`) puis oubliée. Brevo en 429/5xx, réseau coupé, expéditeur non validé (cas réel du 2026-10-13) → le lien de confirmation, le reset, l'invitation à signer ou la quittance ne partent jamais, et personne ne le sait.

**M3. File en mémoire.** Après le commit, l'e-mail attend dans `emailExecutor` (file de 500, en RAM). Un plantage, un `OutOfMemoryError` (`ExitOnOutOfMemoryError`) ou un redéploiement brutal sur Render vident la file. L'arrêt propre (30 s) ne couvre que l'arrêt propre.

**M4. Rejets différés invisibles.** Brevo répond 2xx puis refuse plus tard (expéditeur non validé, rebond). En SMTP, le serveur accepte le message puis peut le renvoyer en rebond. L'application n'en sait rien (point ouvert §7).

**M5. Expéditeur.** `noreplydevback@gmail.com` par défaut dans `application.yml` ; une adresse Gmail comme expéditeur passe mal (DMARC de Gmail). Il faut un domaine à soi avec SPF, DKIM et DMARC.

### 2.2 SMTP générique

Passer à **`spring-boot-starter-mail` (`JavaMailSender`)** configuré uniquement par `spring.mail.*` :

```yaml
spring:
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: ${SMTP_STARTTLS:true}   # 587
      mail.smtp.ssl.enable: ${SMTP_SSL:false}            # 465
      mail.smtp.connectiontimeout: 10000
      mail.smtp.timeout: 20000
      mail.smtp.writetimeout: 20000
kupanga:
  email:
    expediteur: ${EMAIL_EXPEDITEUR}      # noreply@<domaine>
    nom-expediteur: ${EMAIL_NOM_EXPEDITEUR:Kupanga}
    repondre-a: ${EMAIL_REPONDRE_A:}
```

Tout fournisseur marche : **Brevo garde sa place via son relais SMTP** (`smtp-relay.brevo.com:587`), Scaleway TEM, Mailjet, Amazon SES, la boîte e-mail livrée avec un nom de domaine (OVH, Infomaniak…), ou un relais d'hébergeur. Le code n'a plus à changer, seulement les 5 variables.

Points de code : DTO neutre (`EmailSortant` : destinataires, sujet, HTML, **texte brut** en alternative, pièces jointes), `MimeMessageHelper` multipart en UTF-8, en-tête `Message-ID` sur le domaine, `Reply-To`, pas d'adresse ni de sujet dans les logs (W10). Garder une interface `ExpediteurEmail` pour les tests et un éventuel retour à une API HTTP.

> **Attention à « prendre le SMTP de l'hébergeur ».** Un VPS **ne fournit pas** de relais SMTP : il fournit une machine dont le port 25 est souvent bloqué (Hetzner bloque 25 et 465 par défaut, le 587 reste ouvert ; OVH surveille le 25). **Ne pas installer son propre serveur de messagerie** (Postfix) sur le VPS : une IP neuve de VPS a une mauvaise réputation, Gmail et Outlook classeront les e-mails en spam. Utiliser un **relais authentifié sur le port 587** : celui du fournisseur du nom de domaine, ou un service transactionnel. Ce qui compte pour la délivrabilité, c'est **le domaine** (SPF, DKIM, DMARC), pas l'hébergeur.

### 2.3 Ne plus perdre d'e-mail : file persistante et broker

Un broker seul ne suffit pas : si l'application publie dans RabbitMQ **après** le commit et plante entre les deux, l'e-mail est perdu ; si elle publie **avant**, un rollback envoie un e-mail pour une action annulée. La garantie vient du **« transactional outbox »** : l'e-mail est écrit **en base, dans la même transaction** que l'action métier.

**Socle (obligatoire dans tous les cas) — table `emails_sortants`** (nouvelle migration) :

| Colonne | Rôle |
|---|---|
| `id`, `cree_le` | |
| `type` | `VERIFICATION_EMAIL`, `RESET_MOT_DE_PASSE`, `INVITATION_SIGNATURE`, `QUITTANCE`, `BIENVENUE`… |
| `priorite` | `CRITIQUE` (confirmation, reset, invitations, confirmations de signature, quittances) / `NORMALE` (bienvenue, tentative d'inscription) |
| `destinataire`, `sujet`, `html`, `texte` | contenu figé à la création |
| `pieces_jointes` | références `bucket` + `cle` (jamais les octets) |
| `statut` | `A_ENVOYER`, `EN_COURS`, `ENVOYE`, `ECHEC_DEFINITIF`, `PERIME` |
| `tentatives`, `prochain_essai`, `derniere_erreur` | erreur réduite à la classe + code SMTP (ni adresse ni contenu) |
| `expire_le` | un reset ou un lien de signature expiré n'est plus envoyé (`PERIME`) |
| `envoye_le`, `verrou_jusqua` | |

- `EmailServiceImpl` écrit la ligne au lieu de publier l'événement en mémoire.
- Un envoyeur relit les lignes dues (`SELECT … FOR UPDATE SKIP LOCKED`, lot borné), envoie en SMTP, puis passe la ligne à `ENVOYE` ; en erreur temporaire (connexion, 4xx SMTP), **nouvel essai avec délai croissant** : 1 min, 5 min, 15 min, 1 h, 3 h, 6 h, 12 h, 24 h ; en erreur définitive (5xx SMTP « adresse inconnue ») ou après le dernier essai → `ECHEC_DEFINITIF`.
- Un « réveil » juste après le commit (`ApresCommit`) pour que les e-mails partent en quelques secondes, plus une tâche planifiée toutes les 30 s qui rattrape le reste (plantage, redémarrage).
- **Back-office** : page « E-mails » (en attente, en échec, envoyés sur 7 jours), bouton « Renvoyer », compteur d'échecs sur le tableau de bord. **Alerte** (log `ERROR` → Sentry) dès qu'un e-mail `CRITIQUE` passe en `ECHEC_DEFINITIF`.
- **RGPD** : purge du contenu (`html`, `texte`) des e-mails envoyés après 30 jours, des lignes après 90 jours. Le contenu contient des jetons (reset, confirmation) : contenu effacé dès l'envoi pour ces types, ou jeton non stocké (le lien est reconstruit à l'envoi à partir de l'id du jeton).
- Plusieurs instances un jour : `SKIP LOCKED` empêche déjà le double envoi.

**Broker (RabbitMQ) — à décider.** Le socle ci-dessus apporte la garantie « jamais perdu » sans nouveau service. RabbitMQ ajoute :
- la distribution du travail entre plusieurs instances, les files de nouvel essai à délai et une file d'échecs (DLQ) standard ;
- **un relais STOMP** pour la messagerie temps réel quand il y aura plusieurs instances (décision du 2026-10-07 : « relais STOMP (RabbitMQ) ») : le même service servirait aux deux.

Coût : ~150-250 Mo de RAM, un service à surveiller et sauvegarder, et l'outbox reste nécessaire (l'outbox publie dans RabbitMQ, un consommateur envoie).

| Option | Garantie | Coût d'exploitation | Quand |
|---|---|---|---|
| **A. Outbox PostgreSQL seule** (recommandée au lancement) | Aucun e-mail perdu, nouveaux essais, alertes | Aucun service en plus | Une seule instance (cas actuel) |
| B. Outbox + RabbitMQ | Idem + répartition entre instances + base du relais STOMP | +1 service (RAM, sauvegarde de la config, mises à jour) | Dès qu'on passe à 2 instances de l'API |

Le code de l'option A est conçu pour passer à B sans réécriture : interface `FileEmails` (implémentation `FileEmailsPostgres`, plus tard `FileEmailsRabbit` qui publie depuis l'outbox). **Choix à faire par l'utilisateur au début de la tâche M3** (la skill pose la question).

### 2.4 Rebonds et rejets différés (M4)

- Contrôle au démarrage : connexion SMTP de test (`JavaMailSenderImpl.testConnection()`), journalisée en `ERROR` si elle échoue (sans bloquer le démarrage).
- Adresse de retour (`Return-Path` / `mail.smtp.from`) sur une boîte du domaine relevée par le fournisseur ; webhooks de rebond propres à chaque fournisseur : **hors périmètre** (on ne veut pas de dépendance à un fournisseur). À reconsidérer si le volume grandit.

---

## 3. Hébergement : tout le back-end sur un serveur

### 3.1 Ce qui tourne aujourd'hui et ce qu'il faut

| Service | Aujourd'hui | Cible |
|---|---|---|
| API Spring Boot | Render gratuit (512 Mo, tas 300 Mo, mise en veille, réveillé par UptimeRobot) | Conteneur sur le VPS, tas 1 Go |
| PostgreSQL + PostGIS | Neon (serverless) | Conteneur `postgis/postgis` figé, volume local |
| Redis (cache, limites A3) | Upstash (TLS, facturé à la commande, ~6 commandes par login) | Conteneur `redis` local, sans TLS sur le réseau Docker privé |
| Fichiers | MinIO sur VM Oracle Always Free (`minio-backend.duckdns.org`) | RustFS sur le VPS, `fichiers.<domaine>` |
| E-mails | API Brevo | Relais SMTP (Brevo SMTP ou autre) |
| Front Angular | URL de l'API Render en dur (`environment.prod.ts`, `ngsw-config.json`) | Servi par le VPS ou Cloudflare Pages ; **les deux URL à changer** |
| TLS, reverse proxy | Render | **Caddy** (certificats Let's Encrypt automatiques) |
| CDN, anti-DDoS, DNS | — | **Cloudflare** (gratuit) devant le VPS |
| Erreurs | Sentry | Sentry (inchangé) |
| Sauvegardes | Manuelles | Automatiques, chiffrées, hors site (§4) |
| Supervision | UptimeRobot (keep-alive) | UptimeRobot ou Uptime Kuma (alertes réelles), alertes disque |

### 3.2 Dimensionnement

| Composant | Mémoire visée |
|---|---|
| API (tas 1 Go + métaspace, threads, PDF, décodage d'images R1) | 1,5 Go |
| PostgreSQL + PostGIS | 1 Go (`shared_buffers` 256 Mo) |
| RustFS | 0,5 Go |
| Redis (`maxmemory 128mb`) | 0,2 Go |
| Caddy | 0,1 Go |
| RabbitMQ (si option B) | 0,3 Go |
| Système, cache disque, marge pour les pics | 2 Go et plus |

**4 Go suffisent au strict minimum sans RabbitMQ ; 8 Go sont confortables** (pics de génération PDF et de décodage d'images, cache disque de PostgreSQL). Disque : 75-100 Go NVMe (photos 1600 px ≈ 300 Ko + vignette 30 Ko ; 1 000 biens × 10 photos ≈ 3,5 Go).

### 3.3 Proposition de serveur

**Recommandation : un VPS OVHcloud en France (gamme VPS 2026, modèle à ~8 Go de RAM, 4 vCPU, ~75 Go NVMe), Ubuntu 24.04 LTS, Docker Compose, avec Cloudflare devant.**

Pourquoi :
- **Données en France** chez une société française : simple à justifier pour le RGPD et rassurant pour les clients européens ; support et facturation en français.
- Trafic illimité, anti-DDoS inclus, sauvegardes ou instantanés en option.
- Latence correcte vers Kinshasa et Brazzaville (les câbles sous-marins atterrissent surtout vers l'Europe) ; Cloudflare a des points de présence en Afrique pour les photos et le front.
- Port 587 sortant ouvert : un relais SMTP authentifié fonctionne.

Alternative moins chère à configuration égale selon les grilles : **Hetzner Cloud** (Allemagne ou Finlande, RGPD UE). Ports 25 et 465 bloqués par défaut, 587 ouvert (suffisant pour un relais SMTP).

> **Prix à vérifier sur le site officiel** : OVHcloud et Hetzner ont augmenté leurs tarifs plusieurs fois en 2026 (hausse de la RAM et des disques) et les sources tierces se contredisent. Ordre de grandeur relevé : **~5 à 15 € HT/mois** pour 4 à 8 Go de RAM chez l'un ou l'autre. Ajouter ~1 à 5 €/mois pour le stockage des sauvegardes hors site (§4).

Budget mensuel total visé : **~10 à 20 €** (VPS + sauvegarde hors site), domaine en sus, e-mails gratuits tant que le volume tient dans l'offre gratuite du relais choisi (Brevo SMTP : 300 e-mails/jour).

### 3.4 Points d'attention du déploiement

- **IP réelle derrière Cloudflare + Caddy** (impact direct sur A3) : Tomcat fait confiance à Caddy (IP Docker privée) et lit le dernier `X-Forwarded-For`… qui sera **une IP de Cloudflare**. Tous les utilisateurs partageraient alors les limites par IP. Configurer Caddy (`trusted_proxies` avec les plages Cloudflare, `client_ip_headers CF-Connecting-IP`) pour transmettre la vraie IP, et **fermer les ports 80/443 du VPS à tout sauf Cloudflare** (pare-feu), sinon un client contourne Cloudflare et forge l'en-tête.
- **WebSocket** via Cloudflare : supporté ; heartbeat W4 (5 s / 10 s) inférieur au délai d'inactivité de Cloudflare (100 s). Vérifier en recette.
- **Taille des envois** : Cloudflare gratuit accepte 100 Mo par requête (> 50 Mo de B3) ; Caddy : `request_body max_size` 50 Mo sur `POST /biens`, 12 Mo sur `/auth/register`, 1 Mo ailleurs (point ouvert B3).
- **Trois noms** : `api.<domaine>` (API + `/ws`), `fichiers.<domaine>` (photos publiques et URL présignées, **domaine distinct du front**, cf. B5), `<domaine>` ou `app.<domaine>` (front). Les URL présignées sont signées avec l'hôte public : Caddy ne doit pas réécrire l'en-tête `Host` vers RustFS, et Cloudflare **ne doit pas mettre en cache** les URL présignées (règle de cache sur la présence de `X-Amz-Signature`).
- **Ports** : seuls 22 (SSH par clé, de préférence limité à son IP ou derrière Tailscale), 80 et 443 ouverts. PostgreSQL, Redis, RustFS (API et console) et RabbitMQ **jamais exposés** ; console RustFS par tunnel SSH.
- **Mémoire de l'API** : revenir de `-Xms128m -Xmx300m` (Render) à une valeur adaptée (`-Xmx1g` ou `MaxRAMPercentage=75` avec une limite de conteneur à 1,5 Go). Le test `DockerfileTest` accepte les deux formes.
- **Redis sans TLS** sur le réseau Docker interne : `application-prod.yml` exige aujourd'hui `rediss://` (commentaire Upstash) → accepter `redis://redis:6379` + mot de passe.
- **Front** : `environment.prod.ts` et `ngsw-config.json` contiennent `https://kupanga-api.onrender.com` : à changer **ensemble** (un test du front les compare), sinon le cache hors ligne se coupe sans message (point R2).
- **Migrations** : la procédure `docs/sauvegardes.md` §4 (répétition V36-V50+ sur base jetable, `flyway repair`) s'applique au moment de la bascule de Neon vers le VPS.
- **Mises à jour** : `unattended-upgrades` pour l'OS ; images Docker figées par version, mises à jour volontaires (Renovate ou Dependabot, D7/D9).

---

## 4. Sauvegardes et reprise

Le passage en auto-hébergement rend l'utilisateur responsable de ses données. Minimum avant d'accueillir de vrais clients :

- **PostgreSQL** : `pg_dump -Fc` quotidien (rétention 7 jours + 4 semaines + 12 mois) ; WAL archivés en continu (pgBackRest ou `wal-g`) si l'on vise moins de 24 h de perte.
- **Fichiers** : `rclone sync` quotidien de RustFS vers un stockage **chez un autre fournisseur** (Hetzner Storage Box, Backblaze B2, Scaleway, OVH Object Storage), **chiffré** (`rclone crypt`).
- **Config** : `docker-compose-prod.yml`, `Caddyfile`, scripts dans le dépôt ; `.env.prod` dans un gestionnaire de secrets (jamais dans git, P0-8).
- **Test de restauration** chaque mois sur une machine jetable, avec une procédure écrite et chronométrée (objectif : service rétabli en moins de 4 h sur un VPS neuf).
- Alerte si une sauvegarde échoue ou n'a pas tourné depuis 26 h (`healthchecks.io` gratuit ou Uptime Kuma).

---

## 5. Prêt pour de vrais clients : ce qui reste hors infrastructure

Rappel des bloquants déjà notés dans `.claude/CLAUDE.md` §7, à faire en parallèle :

- **RGPD** : CGU, politique de confidentialité (tuiles OSM, numéro communiqué à l'autre partie, adresse publique, durée de conservation des reçus et des sauvegardes), registre des traitements, export et suppression de compte (« Ensuite »).
- **Congo** : validation juridique des modèles `cd-v1` puis retrait du bandeau ; politique d'usage de Nominatim (User-Agent avec contact, 1 req/s).
- **Recette** : IP réelle (§3.4), URL présignées depuis le navigateur, Safari iOS (signature, PWA), photos HEIC, parcours d'inscription complet avec le nouveau relais SMTP.
- **Domaine e-mail** : SPF, DKIM, DMARC (`p=none` puis `quarantine`) sur le domaine expéditeur.

---

## 6. Plan d'action

### Sprint 5 — Stockage S3 et e-mails fiables (code) · skill `/sprint-5-infra`

| Ordre | ID | Tâche |
|---|---|---|
| 1 | `S1` | Stockage S3 portable : SDK AWS v2, package `stockage`, config `kupanga.stockage.*`, plus aucune politique posée à l'envoi, buckets créés et contrôlés au démarrage |
| 2 | `S2` | Clés en base au lieu d'URL (photos de biens, vignettes, photo de profil) + migration Flyway d'extraction ; photos de profil privées + URL présignée |
| 3 | `S3` | RustFS en dev, en CI et dans les tests (image figée) ; script de copie des objets MinIO → RustFS (`rclone`) |
| 4 | `M1` | E-mails en SMTP générique (`spring-boot-starter-mail`), DTO neutre avec texte brut, config `spring.mail.*` + `kupanga.email.*`, Brevo par son relais SMTP |
| 5 | `M2` | File persistante `emails_sortants` (outbox) : écriture dans la transaction, envoi avec `SKIP LOCKED`, nouveaux essais à délai croissant, échec définitif, péremption, purge RGPD, jetons non conservés |
| 6 | `M3` | Broker : option A (outbox seule) ou B (RabbitMQ) selon le choix de l'utilisateur |
| 7 | `M4` | Back-office « E-mails » (en attente, échecs, renvoi), alerte Sentry sur échec critique, contrôle SMTP au démarrage |

### Sprint 6 — Serveur de production · skill `/sprint-6-deploiement`

| Ordre | ID | Tâche |
|---|---|---|
| 1 | `H1` | Choix du serveur et du domaine (utilisateur) ; durcissement du VPS (utilisateur non root, SSH par clé, pare-feu, `unattended-upgrades`, fail2ban) — **runbook**, actions manuelles |
| 2 | `H2` | `deploy/docker-compose-prod.yml` (API, PostGIS, Redis, RustFS, Caddy, [RabbitMQ]), `deploy/Caddyfile`, `.env.prod.exemple`, limites mémoire, réseaux, healthchecks, `application-prod.yml` adapté (Redis local, JVM) |
| 3 | `H3` | IP réelle derrière Cloudflare + Caddy, pare-feu limité à Cloudflare, limites de taille par route, en-têtes de sécurité, règles de cache Cloudflare |
| 4 | `H4` | Sauvegardes automatisées (scripts `deploy/sauvegarde/*`, `rclone crypt`, rétention, alerte) + procédure de restauration testée |
| 5 | `H5` | Bascule : migration Neon → PostgreSQL du VPS, MinIO Oracle → RustFS, Upstash → Redis local, DNS, front (URL de l'API), `flyway repair` — **runbook** pas à pas avec retour arrière |
| 6 | `H6` | Supervision : sondes externes, alertes disque/mémoire, rotation des logs Docker, recette finale (§5) |

---

## 7. Ce que cet audit ne couvre pas

- Haute disponibilité (deux serveurs, base répliquée) : inutile au lancement ; le plan de reprise (§4) couvre la panne du serveur.
- Montée de version Spring Boot 3.4/3.5 (« Ensuite » de `CLAUDE.md`), à faire après la bascule.
- Webhooks de rebond propres à un fournisseur d'e-mails (§2.4).
- Choix définitif des prix : à relever sur les sites officiels le jour de la commande.

### Sources consultées (2026-11-02)
- RustFS 1.0 GA : [linuxiac.com](https://linuxiac.com/rustfs-1-0-s3-compatible-object-storage-reaches-ga/), migration MinIO → RustFS : [jacar.es](https://jacar.es/en/migrate-minio-to-rustfs-with-docker/), alternatives à MinIO : [tech-insider.org](https://tech-insider.org/minio-alternatives-2026/)
- Garage, accès anonyme : [git.deuxfleurs.fr #263](https://git.deuxfleurs.fr/Deuxfleurs/garage/issues/263)
- OVHcloud VPS 2026 : [vpsbenchmarks.com](https://www.vpsbenchmarks.com/hosters/ovhcloud/plans/vps-2), hausse des prix : [next.ink](https://next.ink/225720/ovhcloud-augmente-fortement-le-prix-de-ses-vps-2026-et-ipv4/), port 25 : [community.ovhcloud.com](https://community.ovhcloud.com/t/probleme-smtp-bloque/54024)
- Hetzner 2026 : [bitdoze.com](https://www.bitdoze.com/es/hetzner-cloud-precios-2026/), ports bloqués : [findstack.com](https://findstack.com/resources/port-25-blocked-vps)
