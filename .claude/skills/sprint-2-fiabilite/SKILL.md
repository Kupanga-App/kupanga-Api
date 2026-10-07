---
name: sprint-2-fiabilite
description: Sprint 2 de l'audit Kupanga — fiabilité (bugs B*, WebSocket W*, déploiement D1–D8). Argument optionnel : un ID de tâche (ex. B1) ou "tout".
argument-hint: "[ID | tout]"
disable-model-invocation: true
---

# Sprint 2 — Fiabilité

Argument reçu : `$ARGUMENTS`

1. Lis `.claude/CLAUDE.md`, puis **suis exactement** `.claude/sprint-workflow.md` pour chaque tâche.
2. Avant de commencer, vérifie que le Sprint 1 est terminé dans `.claude/CLAUDE.md`. Sinon, signale-le et demande confirmation avant de continuer.
3. Détails : `AUDIT-PRODUCTION.md` §3 (W*), §4 (B*), §5 (D*).

## Ordre des tâches et points d'attention

| Ordre | ID | Tâche | Points d'attention |
|---|---|---|---|
| 1 | `B1` | Le locataire ne voit pas ses quittances | `QuittanceServiceImpl.getQuittancesParLocataire` appelle `findByProprietaireId` : corriger la requête, et ajouter un test |
| 2 | `A10` | E-mails insensibles à la casse | Normaliser en minuscules (`trim` + `toLowerCase(Locale.ROOT)`) à l'inscription, au login, au login Google, au mot de passe oublié et partout où un e-mail sert de clé (`UserRepository.findByMail`, invitations locataire, messagerie). Migration Flyway : passer les e-mails existants en minuscules **après avoir vérifié qu'aucun doublon n'apparaît** (sinon s'arrêter et lister les doublons à l'utilisateur) ; index unique sur `lower(mail)` |
| 3 | `B2` | Clé du cache de géocodage | Inclure l'adresse complète dans la clé (`GeocodingService`) |
| 4 | `B3` | Uploads limités à 1 Mo | `spring.servlet.multipart.max-file-size` / `max-request-size` = 10 Mo |
| 5 | `B5` | Validation des fichiers uploadés | Liste blanche de types MIME (contrôle du contenu réel, pas seulement de l'extension), taille maximale, nom de fichier généré côté serveur |
| 6 | `W1` | Règles de la messagerie | Un des deux participants doit être le propriétaire du bien (`MessageServiceImpl.envoyerMessage`) |
| 7 | `W3` | Contrôle des SUBSCRIBE | `JwtChannelInterceptor` : n'autoriser que `/user/queue/**` |
| 8 | `W4` | Heartbeat WebSocket | `setHeartbeatValue({10000,10000})` + `TaskScheduler` (`WebSocketConfig`) |
| 9 | `W6` | Lecture par conversation | `marquerConversationLue` filtré par `conversationId` (`MessageRepository`) |
| 10 | `W10` | Contenu des messages dans les logs | Passer en `DEBUG`, sans le contenu |
| 11 | `W11-13` | Front : rattrapage, abonnements, file d'envoi | **Dépôt `kupanga-front`** : recharger l'historique dans `onConnect`, supprimer les abonnements `/user/{email}/...`, file locale avec statut « en attente / échec » |
| 12 | `B6` | Machine à états des signatures | Interdire de re-signer un contrat ou un EDL `SIGNE`, vérifier le statut dans `signerLocataire`, taille maximale de `signatureBase64` |
| 13 | `B7-B8` | Erreurs 500 sur les entrées invalides | `getContratParToken` et `valueOf(...)` des enums EDL → 400 ou 404 propres |
| 14 | `B9` | Recherche des POI | 1 retry, timeout de 10 s, cache ; ne pas passer d'entité détachée à `@Async` (passer l'id) |
| 15 | `B11` | E-mails après commit | `@TransactionalEventListener(phase = AFTER_COMMIT)` à la place de `@Async` dans la transaction |
| 16 | `A14` | Vérification de l'e-mail à l'inscription | Token de vérification envoyé via Brevo (gratuit, pas d'OTP SMS), lien vers le front, expiration ; colonne `email_verifie` (migration Flyway, comptes existants et comptes Google vérifiés = `true`). Envoi **après commit** (s'appuie sur B11). **Demander à l'utilisateur** ce qu'un compte non vérifié peut faire (connexion bloquée ? actions limitées ?) et s'il faut un endpoint « renvoyer l'e-mail » (avec rate limiting A3). **Impact front** |
| 17 | `B12` | Cascades dangereuses | Retirer `cascade = ALL` de `User` vers les biens et messages ; définir le comportement à la suppression avec l'utilisateur (anonymisation ?) |
| 18 | `D1-D8` | Docker, actuator, Sentry, Flyway, versions, sauvegardes | D1 port du compose ; D2 Dockerfile (`USER`, `MaxRAMPercentage`, `HEALTHCHECK`) ; D3 actuator `when-authorized` ; D4 Swagger désactivé en prod ; D5 Sentry PII `false` et sample rate 0.1–0.2 ; D6 Flyway `validate-on-migrate` ; D7 versions d'images figées ; D8 sauvegardes : **documenter la procédure** (`pg_dump` + réplication MinIO), leur mise en place sur le serveur est manuelle |

## Fin de sprint
Lance `testeur` sur la suite complète, puis `revue-securite` sur l'ensemble du sprint. Ajoute au Journal de `.claude/CLAUDE.md` une ligne « Sprint 2 terminé ».
