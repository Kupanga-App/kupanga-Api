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
- [ ] P0-3 rôle (et non hash du mot de passe) dans le JWT au refresh (`AuthServiceImpl.java:110`)
- [ ] P0-4 retirer `password` de `UserDTO`
- [ ] P0-2 forgot/reset password : ne plus renvoyer le token, body JSON, message générique (+ A2), révocation des refresh tokens
- [ ] P0-1 `authorizeHttpRequests` + `@PreAuthorize` + entry point 401 (`SecurityConfig.java:97`)
- [ ] P0-5 contrôle de propriété (IDOR) : `affectLocataire`, `creerContrat`, `creerQuittance`, `creerEtatDesLieux`
- [ ] P0-6 DTO public propriétaire sans e-mail, aucune info locataire en public
- [ ] P0-7 buckets MinIO privés pour contrats/EDL/quittances + URL présignées 5 min
- [ ] VALID `@Valid` partout (A1/B4/W2) + regex d'adresse élargie (C3)
- [ ] CORS CORS et origines WebSocket restreints (A11/W5)
- [ ] A3 rate limiting (login, forgot, register, google)
- [ ] EXC gestionnaire d'exceptions générique, plus de 500 (+ A7/A8)
- [ ] TESTS-SECU tests d'intégration sécurité (401 sans token, 403 sur ressource d'autrui)
- [ ] P0-8 **manuel (utilisateur)** : rotation de tous les secrets de `.env.dev` (fuités dans `d39d993`) + purge de l'historique git

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
| 2026-10-07 | Travail des sprints via **une skill par sprint** + 2 agents transverses (`testeur`, `revue-securite`), pas un agent par sprint | Les tâches d'un sprint partagent le même code et contexte ; les agents servent de second regard indépendant et isolent les logs Maven |
| 2026-10-07 | **A10 et A14 ajoutés au Sprint 2** (casse des e-mails, vérification de l'e-mail à l'inscription) | Rapides à corriger ; A14 se fait par e-mail Brevo (gratuit), cohérent avec « pas d'OTP SMS ». Restent non planifiés : A5, A6, A9, A12, A13, W7, W8, D9–D13 |
| 2026-10-07 | `CLAUDE.md` = mémoire partagée du projet, mise à jour automatiquement par Claude | Garder le contexte entre les conversations |

## 7. Points ouverts / questions

- Le dépôt est-il (ou a-t-il été) public ? → détermine l'urgence de la purge d'historique (P0-8).
- Choix du fournisseur SMS/WhatsApp pour l'OTP : reporté (à étudier quand l'app aura des utilisateurs/revenus ; comparer les tarifs +243/+242).
- Hébergement prod (Europe + CDN Cloudflare ?) : non décidé.

## 8. Journal des modifications

> Format : `AAAA-MM-JJ — quoi (fichiers / commit)`. Le plus récent en haut.

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
