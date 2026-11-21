---
name: sprint-6-deploiement
description: Sprint 6 Kupanga — mise en ligne du pilote sur un VPS-1 OVHcloud (Docker Compose + Caddy + Cloudflare), domaine .com OVHcloud, e-mails par la boîte contact@ d'OVHcloud, sauvegardes Cloudflare R2, images construites par GitHub Actions. Argument optionnel : un ID (ex. H2) ou "tout".
argument-hint: "[ID | tout]"
disable-model-invocation: true
---

# Sprint 6 — Mise en ligne du pilote

Argument reçu : `$ARGUMENTS`

1. Lis `.claude/CLAUDE.md` (§6, §7), **`CHOIX-SERVEUR.md`** (choix et budget du pilote, décidés le 2026-11-21) et
   `AUDIT-INFRA.md` (§3 à §5), puis **suis** `.claude/sprint-workflow.md` pour chaque tâche.
   **Les choix de `CHOIX-SERVEUR.md` priment sur `AUDIT-INFRA.md`** quand ils diffèrent (VPS, e-mails, sauvegardes).
2. Vérifie que le Sprint 5 est terminé (stockage S3 portable, SMTP générique, file d'e-mails) : le déploiement en dépend.
3. Une ancienne tentative (Oracle, annulée le 2026-11-03) est archivée dans
   `C:\Users\Leclerc\kupanga-archive-deploy-2026-11-03\`. Ne la consulter **qu'avec l'accord de l'utilisateur**, comme
   simple référence ; **ne jamais en recopier les secrets** (`.env.prod`, `terraform.*`), tous caducs.

## Pile retenue (ne pas la remettre en question sans l'utilisateur)

| Élément | Choix | Détail à respecter |
|---|---|---|
| Serveur | **VPS-1 OVHcloud** : 2 vCores, 4 Go, 40 Go NVMe, Ubuntu 24.04 LTS, Gravelines ou Strasbourg | sauvegarde quotidienne OVHcloud incluse (la vérifier active) ; **swap de 2 Go** ; passer au VPS-2 si la RAM ou le CPU restent > 70 % |
| Domaine | **`.com` chez OVHcloud** ; DNS délégué à **Cloudflare** (gratuit) | `<domaine>` (front), `www.<domaine>` (redirection), `api.<domaine>`, `fichiers.<domaine>` en **proxy Cloudflare** (nuage orange) ; enregistrements mail (MX, SPF, DKIM, DMARC) **sans proxy** |
| HTTPS | Cloudflare en mode **Full (strict)** + **certificat d'origine Cloudflare** (15 ans) monté dans Caddy | pas de Let's Encrypt (le pare-feu ne laisse passer que Cloudflare) ; « Always Use HTTPS » et HSTS côté Cloudflare |
| Proxy | **Caddy** (seul conteneur avec un port publié : 443) | `trusted_proxies` = plages Cloudflare, `client_ip_headers CF-Connecting-IP` |
| Front | image `kupanga-front` (Angular build de production servi par un petit serveur de fichiers interne) derrière Caddy | `index.html`, `ngsw-worker.js`, `ngsw.json` **jamais en cache** ; fichiers à empreinte (`*.js`, `*.css`) en cache long ; repli SPA vers `index.html` |
| API | image `kupanga-api` (`Dockerfile` du dépôt), profil `prod` | tas Java **512 Mo** (`-Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError`), `PORT=8089` (retirer la contrainte Render `PORT=10000` et adapter `DockerfileTest`) ; Hikari 10 connexions |
| Base | `postgis/postgis` **version figée** | réglages pour 4 Go : `shared_buffers=256MB`, `effective_cache_size=1GB`, `work_mem=4MB`, `max_connections=30` |
| Cache | `redis` figé, mot de passe | `maxmemory 64mb`, **`volatile-lru`** (les compteurs A3 n'ont pas d'expiration à évincer), pas de persistance obligatoire |
| Fichiers | **RustFS** figé (déjà en dev et en CI) | `fichiers.<domaine>` → RustFS **sans réécrire l'en-tête `Host`** (signature des URL présignées) ; compte de service limité aux 6 buckets (README §Configuration) |
| E-mails | **boîte `contact@<domaine>` OVHcloud comme SMTP** | `SMTP_HOST=smtp.mail.ovh.net`, `SMTP_PORT=465`, `SMTP_SSL=true`, `SMTP_STARTTLS=false`, `SMTP_USERNAME`/`EMAIL_EXPEDITEUR=contact@<domaine>` ; ≈ 200 e-mails / heure ; SPF + **DKIM OVHcloud** + DMARC dans Cloudflare ; **Brevo ou Resend seulement en secours** (changement de variables) |
| Sauvegardes | **Cloudflare R2** (gratuit jusqu'à 10 Go) + sauvegarde quotidienne OVHcloud | `rclone` + `rclone crypt`, jeton R2 limité au seul bucket de sauvegarde |
| Images | **GitHub Actions** construit et pousse `ghcr.io/<compte>/kupanga-api:<sha>` et `…/kupanga-front:<sha>` | **rien ne se compile sur le VPS** (4 Go ne suffisent pas pour Maven + Angular) ; le VPS tire les images avec un jeton en lecture seule |
| Supervision | **UptimeRobot** (ou Better Stack) gratuit, **Sentry** gratuit, **healthchecks.io** gratuit | sondes hors du VPS ; signal de vie des sauvegardes |

Mémoire visée sur le VPS-1 (limites `mem_limit` du compose) : `api` 900 Mo, `db` 700 Mo, `rustfs` 400 Mo,
`redis` 96 Mo, `caddy` 64 Mo, `front` 32 Mo : ≈ 2,2 Go, le reste pour le système et le cache disque.

## Règles propres à ce sprint

- **Claude n'agit jamais sur le serveur, le DNS, Cloudflare, OVHcloud, GitHub ni les comptes des fournisseurs.** Il écrit
  les fichiers (`deploy/…`, `.github/workflows/…`) et des **runbooks** pas à pas que l'utilisateur exécute ; il lui
  demande de coller les sorties utiles (`! <commande>` ou copie) pour vérifier.
- **Aucun secret dans le dépôt** : `deploy/.env.prod.exemple` avec des valeurs factices et **la liste complète** des
  variables (ci-dessous) ; `.env.prod` dans le `.gitignore` ; le certificat d'origine et sa clé hors du dépôt.
- Images Docker **figées par version** (D7) ; aucun port de PostgreSQL, Redis ou RustFS publié sur l'hôte.
- **Docker contourne `ufw`** pour les ports publiés : filtrer le 443 dans la chaîne `DOCKER-USER` (plages Cloudflare
  seulement), pas uniquement avec `ufw`.
- Chaque runbook a une étape « vérifier » et une étape « revenir en arrière ».
- **Infrastructure et base neuves, aucune donnée à reprendre** (décision du 2026-11-02) : Flyway crée le schéma au
  premier démarrage (V1 à la dernière migration) ; pas de `flyway repair`.
- Tests : tests de contenu des fichiers de config comme `DockerfileTest` / `ProdConfigTest` (aucun `ports:` sauf
  `caddy`, versions figées, `mem_limit` présents, `.env.prod` ignoré) ; `docker compose -f deploy/docker-compose-prod.yml
  config` doit passer avec `.env.prod.exemple`.

### Variables de `deploy/.env.prod.exemple` (toutes)

`DOMAINE`, `SPRING_PROFILES_ACTIVE=prod`, `PORT=8089`, `JAVA_OPTS`, `DB_URL=jdbc:postgresql://db:5432/kupanga`,
`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `REDIS_URL=redis://:<mot de passe>@redis:6379`,
`JWT_SECRET_KEY` (`openssl rand -base64 64`), `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `GOOGLE_CLIENT_ID`,
`FRONTEND_URL=https://<domaine>/`, `CORS_ALLOWED_ORIGINS=https://<domaine>,https://www.<domaine>`,
`STOCKAGE_ENDPOINT=http://rustfs:9000`, `STOCKAGE_URL_PUBLIQUE=https://fichiers.<domaine>`, `STOCKAGE_ACCESS_KEY`,
`STOCKAGE_SECRET_KEY`, `STOCKAGE_REGION`, `STOCKAGE_PATH_STYLE=true`, `STOCKAGE_GERER_POLITIQUES=true`,
`RUSTFS_ACCESS_KEY` / `RUSTFS_SECRET_KEY` (administrateur RustFS, distinct du compte de service), `SMTP_HOST`,
`SMTP_PORT`, `SMTP_SSL`, `SMTP_STARTTLS`, `SMTP_AUTH=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `EMAIL_EXPEDITEUR`,
`EMAIL_NOM_EXPEDITEUR=Kupanga`, `EMAIL_REPONDRE_A`, `EMAIL_ENVOI_INTERVALLE=PT30S` (format ISO-8601 obligatoire),
`SENTRY_DSN`, `IMAGE_API`, `IMAGE_FRONT` (étiquettes `ghcr.io/…:<sha>`). Comparer avec les `${…}` de
`application.yml` et `application-prod.yml` : une variable oubliée empêche le démarrage.

## Ordre des tâches et points d'attention

| Ordre | ID | Tâche | Points d'attention |
|---|---|---|---|
| 1 | `H1` | Comptes, domaine, serveur durci | **Seule question à l'utilisateur** (`AskUserQuestion`) : le **nom de domaine** (et le nom du compte GitHub pour `ghcr.io`) ; le reste est fixé ci-dessus. Écrire `deploy/README.md` (schéma : Cloudflare → Caddy → front / api / rustfs ; db et redis internes) et `deploy/runbooks/01-preparer-serveur.md` : commande du VPS-1 et du `.com` chez OVHcloud ; Ubuntu 24.04 ; utilisateur non root, SSH par clé seulement (mot de passe et root désactivés) ; `ufw` (22, 443) ; `unattended-upgrades` ; fail2ban ; Docker Engine + plugin Compose depuis le dépôt officiel ; fuseau UTC ; swap de 2 Go ; délégation du DNS à Cloudflare, enregistrements `A` proxifiés, MX / SPF de la boîte OVHcloud non proxifiés ; SSL Full (strict) et certificat d'origine ; vérification que le VPS joint `smtp.mail.ovh.net:465` (`nc -vz`) |
| 2 | `H2` | Images et compose de production | `.github/workflows/` : construction et publication des images `kupanga-api` (dépôt API) et `kupanga-front` (dépôt `kupanga-front`, build `production`) sur `ghcr.io` à chaque étiquette `v*` ou sur demande, après les tests ; `Dockerfile` API revu (tas 512 Mo, port 8089, `DockerfileTest`) ; `Dockerfile` du front. `deploy/docker-compose-prod.yml` : `caddy` (seul `ports: 443`), `front`, `api`, `db`, `redis`, `rustfs` + service ponctuel d'initialisation des buckets et du compte de service RustFS ; réseau interne, `restart: unless-stopped`, healthchecks (`/actuator/health/liveness` pour l'API), `mem_limit`, journaux `json-file` avec `max-size`. `deploy/Caddyfile` : `<domaine>` → front, `www` → redirection, `api.<domaine>` → api (WebSocket `/ws` compris), `fichiers.<domaine>` → rustfs sans réécrire `Host` ; en-têtes HSTS, `X-Content-Type-Options`, `Referrer-Policy`, compression. `application-prod.yml` : Redis interne `redis://` avec mot de passe (plus d'exigence `rediss://`), Hikari 10. `deploy/deployer.sh <étiquette>` : `docker compose pull` puis `up -d`, attente de la santé, retour à l'étiquette précédente si échec |
| 3 | `H3` | IP réelle, Cloudflare, pare-feu, limites | Caddy : `trusted_proxies` = plages Cloudflare IPv4 + IPv6 (script de mise à jour), `client_ip_headers CF-Connecting-IP`, IP transmise à l'API ; Tomcat (`forward-headers-strategy: native`, proxy interne de confiance) lit bien cette IP : **test d'intégration** si possible. `deploy/pare-feu/cloudflare-docker-user.sh` : 443 ouvert **aux seules plages Cloudflare** dans `DOCKER-USER`. Limites de corps dans Caddy (50 Mo `POST /biens`, 12 Mo `/auth/register`, 10 Mo reçus, 1 Mo ailleurs ; Cloudflare gratuit plafonne à 100 Mo). Règles Cloudflare documentées : cache des photos publiques (noms UUID immuables), **aucun cache** sur `api.`, sur les URL présignées (`X-Amz-Signature`), `index.html`, `ngsw-worker.js`, `ngsw.json` ; WebSocket activé. Option gratuite conseillée : **Cloudflare Access** devant `api.<domaine>/backoffice` (code par e-mail, en plus du login admin). Reprendre la recette A3 « IP réelle » de `CLAUDE.md` §7 |
| 4 | `H4` | E-mails du domaine | `deploy/runbooks/03-emails.md` : boîte `contact@<domaine>` dans l'espace client OVHcloud ; enregistrements **MX** et **SPF** d'OVHcloud ; activation de la signature **DKIM** de la boîte et recopie de ses enregistrements dans Cloudflare ; **DMARC** `p=none` avec adresse de rapports, à durcir (`quarantine`) après deux semaines sans problème ; variables `SMTP_*` / `EMAIL_*` ; contrôle : démarrage sans ERROR `VerificationSmtp`, inscription réelle → e-mail reçu dans Gmail et Outlook (pas en spam), score **mail-tester.com ≥ 9/10**, page E-mails du back-office sans échec. **Secours** : procédure de bascule vers Brevo ou Resend (domaine à authentifier chez eux, variables à changer, rien d'autre) |
| 5 | `H5` | Sauvegardes | `deploy/sauvegarde/` : `sauvegarder-postgres.sh` (`pg_dump -Fc` depuis le conteneur, rétention 7 jours / 4 semaines / 12 mois), `sauvegarder-fichiers.sh` (`rclone sync` des données RustFS vers R2 via `rclone crypt`), timers `systemd` (vers 02:30 UTC), ping **healthchecks.io** à la fin de chaque sauvegarde (alerte si rien depuis 26 h), clé de chiffrement rclone conservée **hors du VPS** (gestionnaire de mots de passe). `deploy/runbooks/04-restaurer.md` : restauration complète sur un VPS neuf, chronométrée, **faite une fois avant l'ouverture du pilote** puis chaque mois. Vérifier aussi la sauvegarde quotidienne OVHcloud. Mettre à jour `docs/sauvegardes.md` |
| 6 | `H6` | Mise en service et recette | `deploy/runbooks/05-mise-en-service.md` : (1) premier démarrage sur base vide (Flyway, buckets créés, contrôle SMTP dans les logs) ; (2) **front** : `environment.prod.ts` (`apiUrl`, `wsUrl`) **et** `ngsw-config.json` vers `https://api.<domaine>` (dépôt `kupanga-front`, le test qui les compare doit passer) ; pages légales (`EDITEUR`, liens vers le domaine) ; (3) **Google OAuth** : origine JavaScript `https://<domaine>`, Branding avec les liens légaux du domaine, Search Console, publication de l'application (cf. `CLAUDE.md` §7 « Connexion Google ») ; (4) contrôles : inscription → e-mail reçu → confirmation → connexion ; connexion Google ; création d'un bien avec photo ; bail signé et PDF ouvert (URL présignée) ; quittance ; messagerie en temps réel à travers Cloudflare ; `curl` anonyme sur un avatar → 403 ; login 429 depuis deux IP différentes (IP réelle) ; PWA installable ; back-office ; (5) **supervision** : UptimeRobot sur `https://<domaine>` et `https://api.<domaine>/actuator/health/liveness` (5 min), Sentry reçoit une erreur de test, alerte disque > 80 % ; (6) retour arrière : étiquette d'image précédente (`deployer.sh`), rien à restaurer au premier lancement. Checklist « prêt pour le pilote » cochée avec l'utilisateur ; ce qui reste ouvert va en §7 de `CLAUDE.md` |

## Fin de sprint

Lance `testeur` (suite complète : `Dockerfile`, `application-prod.yml` et workflows ont changé ; front : `ng build`
et `ng test`), puis `revue-securite` sur `deploy/`, `.github/workflows/`, les `Dockerfile` et la config de prod
(secrets, ports, pare-feu, en-têtes, Cloudflare Access). Ajoute au Journal de `.claude/CLAUDE.md` une ligne
« Sprint 6 (mise en ligne du pilote) terminé », mets à jour §2 (hébergement réel) et le README (schéma
d'architecture, variables de production).
