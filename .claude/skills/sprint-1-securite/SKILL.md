---
name: sprint-1-securite
description: Sprint 1 de l'audit Kupanga — corrige les failles de sécurité bloquantes (P0-1 à P0-8, validation, CORS, rate limiting, exceptions, tests de sécurité). Argument optionnel : un ID de tâche (ex. P0-3) ou "tout".
argument-hint: "[ID | tout]"
disable-model-invocation: true
---

# Sprint 1 — Sécurité (bloquant avant toute mise en ligne)

Argument reçu : `$ARGUMENTS`

1. Lis `.claude/CLAUDE.md`, puis **suis exactement** `.claude/sprint-workflow.md` pour chaque tâche.
2. Détails techniques de chaque item : `AUDIT-PRODUCTION.md` §1 (P0), §2 (A*), §3 (W*), §4 (B*).

## Ordre des tâches et points d'attention

| Ordre | ID | Tâche | Points d'attention |
|---|---|---|---|
| 1 | `P0-3` | Rôle (et non le hash du mot de passe) dans le JWT au refresh | `AuthServiceImpl` ~l.110. Test : le claim `role` après refresh = rôle de l'utilisateur |
| 2 | `P0-4` | Retirer `password` de `UserDTO` | `UserDTO`, `UserMapper`, exemples Swagger. Chercher tous les DTO et mappers qui exposent encore `password` |
| 3 | `P0-2` | Mot de passe oublié / réinitialisation | Message générique (inclut **A2** : pas d'énumération), `token` + `newPassword` dans un **body JSON**, validation de la robustesse du mot de passe, révocation des refresh tokens. **Impact front** à signaler |
| 4 | `P0-1` | `authorizeHttpRequests` + entry point 401 + `@PreAuthorize` | Base : liste de l'audit §1. Garder publics : signature par token, `GET /biens/*`, `POST /biens/search`, `/ws/**`, `/actuator/health`. Ne pas casser la chaîne back-office `@Order(1)`. Swagger : décider avec l'utilisateur (ADMIN ou désactivé en prod) |
| 5 | `P0-5` | Contrôle de propriété (IDOR) | Méthode commune `verifierProprietaire(bienId, email)` appelée **dans les services** : `affectLocataire`, `creerContrat`, `creerQuittance`, `creerEtatDesLieux`. Vérifier aussi `emailLocataire` = locataire réel, et `contratId` de la quittance = même bien. Chercher les autres méthodes du même type |
| 6 | `P0-6` | Vue publique des biens sans données personnelles | `ProprietairePublicDTO` (prénom + initiale du nom + photo), aucune info locataire. `BienMapper.mapProprietairePublic`. **Impact front** |
| 7 | `P0-7` | Buckets MinIO privés + URL présignées | Contrats, EDL, quittances (et futur reçu C8-bis) en privé. Stocker la **clé** d'objet, pas l'URL (migration Flyway si besoin pour les données existantes). Endpoint authentifié qui génère une URL présignée de 5 min. PDF en base64 pour la pièce jointe Brevo. **Impact front** |
| 8 | `VALID` | `@Valid` partout (A1, B4, W2) | Inscription, création de bien, `MessagePayload` (`@Size(max=4000)`, `@NotBlank`) + `@MessageExceptionHandler` vers `/user/queue/errors`. **Élargir en même temps la regex d'adresse (C3 : `°`, `/`, `#`, `&`)**, sinon les adresses congolaises sont refusées |
| 9 | `CORS` | CORS et origines WebSocket restreints (A11, W5) | Origines lues dans la config (`spring.websocket.allowed-origins` existe déjà), plus de `*` avec `allowCredentials(true)` |
| 10 | `A3` | Rate limiting | Login, forgot-password, register, google. Proposer Bucket4j (avec Redis, déjà présent) ou une limite côté reverse proxy : **demander le choix** à l'utilisateur |
| 11 | `EXC` | Gestionnaire d'exceptions générique | `GlobalExceptionHandler` : `Exception`, `AccessDeniedException`, `MaxUploadSizeExceededException`, `IllegalStateException`, `IllegalArgumentException`, `MissingRequestCookieException` (inclut **A7**, **A8**). Aucune stacktrace dans la réponse |
| 12 | `TESTS-SECU` | Tests d'intégration de sécurité | Pour chaque endpoint : 401 sans token, 403 sur la ressource d'un autre utilisateur, endpoints publics accessibles. Peut révéler des trous restants : les corriger dans le périmètre de P0-1/P0-5 |
| 13 | `P0-8` | Secrets fuités dans l'historique git | **Tâche manuelle.** Ne rien exécuter. Donner à l'utilisateur la checklist : changer la clé JWT, le mot de passe PostgreSQL, les clés MinIO, la clé Brevo, le secret Google et le mot de passe admin ; puis, si le dépôt est ou a été public, purger l'historique avec `git filter-repo` (réécriture irréversible, à faire par lui). Cocher seulement quand il confirme |

## Fin de sprint
Quand toutes les cases du Sprint 1 sont cochées, lance `testeur` sur la suite complète, puis `revue-securite` sur l'ensemble du sprint (`git diff` depuis le début du sprint, demander la référence à l'utilisateur si besoin). Ajoute au Journal de `.claude/CLAUDE.md` une ligne « Sprint 1 terminé ».
