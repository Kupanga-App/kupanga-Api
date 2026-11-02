# Guide de démarrage — Kupanga API

Ce guide part de zéro : un poste vide, aucun compte, aucune donnée. À la fin, l'API tourne en local avec sa base,
son stockage de fichiers et ses e-mails de test, et vous avez créé et confirmé un premier compte.

Compter 30 à 45 minutes la première fois (téléchargements compris).

---

## 1. Installer les outils

| Outil | Version | Pourquoi | Vérifier |
|---|---|---|---|
| Git | récente | récupérer le code | `git --version` |
| Docker Desktop (Windows / macOS) ou Docker Engine + Compose (Linux) | récente | base, stockage, Redis, e-mails de test, et l'API elle-même | `docker compose version` |
| JDK 21 (Eclipse Temurin conseillé) | 21 | compiler et lancer les tests sans Docker | `java -version` |
| Un IDE Java (IntelliJ IDEA conseillé) | — | travailler sur le code | — |
| Node.js 20+ et npm | — | seulement pour le front `kupanga-front` | `node -v` |

> **Windows** : activez WSL 2 dans Docker Desktop (Paramètres → General → « Use the WSL 2 based engine »), puis
> laissez Docker Desktop **lancé** pendant tout le guide. Les commandes ci-dessous marchent dans Git Bash ou
> PowerShell. Maven n'a pas besoin d'être installé : le dépôt fournit `./mvnw` (`mvnw.cmd` sous PowerShell).

---

## 2. Récupérer le code

```bash
git clone https://github.com/Kupanga-App/kupanga-Api.git
cd kupanga-Api
```

Le front Angular est dans un autre dépôt (`kupanga-front`), facultatif pour ce guide (§8).

---

## 3. Créer le fichier `.env.dev`

L'API et la base lisent leurs réglages de développement dans `.env.dev` (jamais commité). Partez du modèle :

```bash
cp .env.dev.exemple .env.dev
```

Puis ouvrez `.env.dev` et changez **deux valeurs** :

1. **`JWT_SECRET_KEY`** — clé aléatoire en base64 (au moins 32 octets) :
   ```bash
   openssl rand -base64 48
   ```
   ou, dans PowerShell :
   ```powershell
   [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
   ```
   Collez le résultat après `JWT_SECRET_KEY=`.
2. **`ADMIN_PASSWORD`** — mot de passe du back-office (avec `ADMIN_EMAIL`, c'est le seul compte administrateur).

Le reste peut rester tel quel :

- `POSTGRES_*` et `DB_URL` : base locale créée au premier démarrage ;
- `GOOGLE_CLIENT_ID` : vide = pas de connexion Google (tout le reste fonctionne). Pour l'activer : Google Cloud
  Console → API et services → Identifiants → « ID client OAuth » de type *Application Web*, origine JavaScript
  autorisée `http://localhost:4200` ;
- **rien à régler pour les fichiers ni pour les e-mails** : `docker-compose-dev.yml` fournit un stockage S3 local
  (RustFS) et un faux serveur SMTP (Mailpit) qui capture les e-mails sans les envoyer.

---

## 4. Démarrer tout l'environnement

```bash
docker compose -f docker-compose-dev.yml up -d --build
```

La première fois, Docker télécharge les images et compile l'API (5 à 10 minutes). Au démarrage, l'API :

- crée le schéma de la base (Flyway joue les migrations `V1` à `V52` sur la base vide) ;
- crée les buckets du stockage et règle leurs droits (photos des biens publiques, documents et photos de profil
  privés) ;
- vérifie la connexion au serveur SMTP (Mailpit).

Suivre le démarrage :

```bash
docker compose -f docker-compose-dev.yml logs -f kupanga
```

C'est prêt quand le log affiche `Started ApiApplication`. Contrôle rapide :

```bash
curl http://localhost:8089/actuator/health
# {"status":"UP", ...}
```

### Ce qui tourne

| Service | Adresse | Identifiants |
|---|---|---|
| API | http://localhost:8089 | — |
| Swagger (documentation interactive de l'API) | http://localhost:8089/swagger-ui.html | — |
| Back-office | http://localhost:8089/backoffice/login | `ADMIN_EMAIL` / `ADMIN_PASSWORD` de `.env.dev` |
| Mailpit (e-mails reçus) | http://localhost:8025 | — |
| Console RustFS (fichiers) | http://localhost:9001 | `kupanga-dev` / `kupanga-dev-secret` |
| API S3 RustFS | http://localhost:9000 | idem |
| PostgreSQL + PostGIS | `localhost:5433`, base `kupanga_dev` | `POSTGRES_USER` / `POSTGRES_PASSWORD` de `.env.dev` |
| Redis | `localhost:6379` | — |
| RedisInsight | http://localhost:5540 | ajouter la base `redis://redis-dev:6379` |

RustFS et Mailpit n'écoutent que sur votre machine (`127.0.0.1`).

---

## 5. Premier parcours : créer et confirmer un compte

Sans le front, tout se fait depuis Swagger.

1. **Inscription** — `POST /auth/register` (multipart). Dans Swagger, partie `userFormDTO` :
   ```json
   {
     "firstName": "Grace",
     "lastName": "Mbuyi",
     "mail": "grace@exemple.test",
     "password": "MotDePasse1",
     "role": "ROLE_PROPRIETAIRE",
     "urlAvatar": null,
     "telephone": "+243812345678"
   }
   ```
   Mot de passe : 8 caractères au moins, une majuscule, une minuscule, un chiffre. Rôles : `ROLE_PROPRIETAIRE`
   ou `ROLE_LOCATAIRE`. La photo (`imageProfil`) est facultative. Réponse : **201** (même si l'adresse existe
   déjà : l'API ne révèle jamais si un compte existe).
2. **Confirmer l'adresse** — ouvrez Mailpit (http://localhost:8025) : l'e-mail « Confirmez votre adresse » est
   là. Le lien pointe vers le front (`http://localhost:4200/auth/verifier-email?token=…`). Sans le front,
   copiez la valeur de `token` et appelez `POST /auth/verifier-email` avec `{"token": "…"}`.
3. **Se connecter** — `POST /auth/login` avec `{"email": "grace@exemple.test", "password": "MotDePasse1"}`.
   Copiez `accessToken`, puis bouton **Authorize** de Swagger : `Bearer <accessToken>`. Les routes protégées
   sont maintenant accessibles (créer un bien : `POST /biens`, pays `FR` ou `CD`).
4. **Voir côté administration** — http://localhost:8089/backoffice/login : le compte apparaît dans
   *Utilisateurs* ; l'e-mail envoyé dans *E-mails* (adresse masquée).

> Un compte non confirmé ne peut pas se connecter (403). Le lien de confirmation vaut 24 h ; pour en recevoir un
> autre : `POST /auth/renvoyer-verification`.

---

## 6. Travailler sur le code

### Recompiler après une modification (tout dans Docker)

```bash
docker compose -f docker-compose-dev.yml up -d --build kupanga
```

### Lancer l'API depuis l'IDE (plus rapide pour déboguer)

1. Arrêtez seulement le conteneur de l'API (le reste continue de tourner) :
   ```bash
   docker compose -f docker-compose-dev.yml stop kupanga
   ```
2. Dans IntelliJ : lancer `ApiApplication` avec ces **variables d'environnement** (elles l'emportent sur
   `.env.dev`, qui donne les adresses vues depuis Docker) :
   ```
   SPRING_PROFILES_ACTIVE=dev
   DB_URL=jdbc:postgresql://localhost:5433/kupanga_dev
   SPRING_DATA_REDIS_HOST=localhost
   ```
   Le stockage (`localhost:9000`) et Mailpit (`localhost:1025`) sont déjà les valeurs par défaut du profil `dev`.

### Lancer les tests

Les tests utilisent une base PostGIS à part, en mémoire (port 5434) :

```bash
docker compose -f docker-compose-dev.yml --profile test up -d db-test
./mvnw verify
```

- Les 3 tests sur un vrai serveur S3 (`StockageS3IntegrationTest`) tournent si le RustFS de dev est démarré
  (`localhost:9000`), sinon ils sont ignorés.
- Les tests du limiteur de tentatives sur Redis tournent si Redis est démarré.
- La CI GitHub Actions (`.github/workflows/ci.yml`) fait la même chose à chaque push : PostGIS, RustFS, Redis,
  migrations Flyway, `./mvnw verify`.

---

## 7. Repartir de zéro

Tout effacer (base, fichiers, cache) et recommencer à l'étape 4 :

```bash
docker compose -f docker-compose-dev.yml down -v
docker compose -f docker-compose-dev.yml up -d --build
```

`down -v` supprime les volumes : **toutes les données locales sont perdues** (c'est le but).

> Si vous aviez lancé une ancienne version du projet (avec MinIO), un ancien volume `…_minio-data-dev` peut
> rester : `docker volume ls` puis `docker volume rm <nom>`.

---

## 8. Le front (facultatif)

```bash
git clone https://github.com/Kupanga-App/kupanga-front.git
cd kupanga-front
npm install
npx ng serve
```

Ouvrir http://localhost:4200. Le front appelle l'API sur `http://localhost:8089`
(`src/environments/environment.ts`). Les liens des e-mails (confirmation, mot de passe oublié, signature)
pointent vers ce front.

---

## 9. Problèmes fréquents

| Symptôme | Cause et solution |
|---|---|
| `error during connect … dockerDesktopLinuxEngine` | Docker Desktop n'est pas lancé. |
| `port is already allocated` (5433, 6379, 8089, 9000…) | Un autre programme ou un ancien conteneur occupe le port : `docker ps`, puis l'arrêter. |
| L'API redémarre en boucle, `Illegal base64 character` ou erreur sur `jwt` | `JWT_SECRET_KEY` n'est pas une vraie clé base64 (§3). |
| `Could not resolve placeholder 'ADMIN_PASSWORD'` | `.env.dev` absent ou incomplet (§3). |
| Aucun e-mail dans Mailpit | L'API envoie après la validation de l'action : attendre quelques secondes ; sinon le back-office → *E-mails* montre l'erreur. |
| Une photo envoyée n'apparaît pas | Console RustFS (http://localhost:9001) : le bucket `bucket-photo-immobilier` doit exister ; redémarrer l'API le recrée. |
| Depuis l'IDE : `UnknownHostException: db` ou `redis` | Variables d'environnement de §6 manquantes. |
| Tests en erreur `Connection refused … 5434` | `db-test` n'est pas démarré (§6). |

---

## 10. Pour aller plus loin

- `README.md` : fonctionnalités, architecture, configuration.
- `AUDIT-PRODUCTION.md` et `AUDIT-INFRA.md` : état de préparation à la production.
- `.claude/CLAUDE.md` : décisions, avancement et points ouverts du projet.
- Mise en production sur un serveur : Sprint 6 (`AUDIT-INFRA.md` §3), pas encore réalisé.
