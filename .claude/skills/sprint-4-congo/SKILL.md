---
name: sprint-4-congo
description: Sprint 4 Kupanga — fonctionnalités Congo (repère carte et géocodage optionnel, téléphone, liens WhatsApp wa.me, email_verified Google, suivi manuel des paiements C8-bis, types de biens et équipements, compression images, PWA). Argument optionnel : un ID (ex. C6) ou "tout".
argument-hint: "[ID | tout]"
disable-model-invocation: true
---

# Sprint 4 — Fonctionnalités Congo (MVP Kinshasa)

Argument reçu : `$ARGUMENTS`

1. Lis `.claude/CLAUDE.md` (§4bis et §6), puis **suis exactement** `.claude/sprint-workflow.md` pour chaque tâche.
2. Vérifie que le Sprint 3 (socle juridiction) est terminé. Les tâches ci-dessous **s'appuient sur les profils de juridiction** : tout ce qui varie selon le pays passe par `JuridictionProperties` / `RegleJuridiction`.
3. Décisions à respecter absolument : **aucun paiement via l'app**, **pas d'OTP SMS/WhatsApp au MVP**, téléphone = simple contact non vérifié.

## Ordre des tâches et points d'attention

| Ordre | ID | Tâche | Points d'attention |
|---|---|---|---|
| 1 | `A4` | `email_verified` à la connexion Google | `GoogleTokenVerifierImpl` + `AuthServiceImpl.loginWithGoogle` : refuser de lier un compte existant si l'e-mail n'est pas vérifié |
| 2 | `C6` | Champ `telephone` | Format E.164 (+243, +242, +33…), facultatif, **sans vérification** ; profil utilisateur, DTO, mapper, migration Flyway |
| 3 | `C7-C16` | Liens WhatsApp `wa.me` | Le back génère le texte et l'URL `https://wa.me/<numéro sans +>?text=<texte encodé>` pour l'invitation à signer (contrat, EDL) et le rappel de loyer ; activé selon le canal `WHATSAPP_LIEN` du profil. Aucune API WhatsApp payante. **Front** : bouton « Envoyer par WhatsApp » |
| 4 | `C2` | Repère sur la carte + géocodage optionnel | Le géocodage qui échoue ne bloque plus la création (B10) ; coordonnées fournies par le client (repère sur la carte ou GPS) acceptées et validées (bornes lat/lon) ; repli possible sur le centre de la commune. **Front** : sélection sur la carte |
| 5 | `C8-bis` | Suivi manuel des paiements | Le propriétaire déclare le loyer reçu : date, mode (`ESPECES`, `MOBILE_MONEY`, `VIREMENT`, `AUTRE`), photo du reçu facultative dans un bucket MinIO **privé** (URL présignée, cf. P0-7). Contrôle de propriété (P0-5). **Aucun** flux d'argent ni intégration de paiement |
| 6 | `C10-C13` | Types de biens, équipements, garantie + avance, compteurs | Types : villa, parcelle, annexe, chambre, local commercial, entrepôt, terrain (filtrés par le profil). Équipements : eau, électricité (SNEL, groupe électrogène, solaire, heures/jour), clôture, gardiennage, parking, climatisation. Garantie (en mois) **et** loyers payés d'avance, en deux champs. Compteurs prépayés (Cash Power) et groupe électrogène dans l'EDL. **Valider la liste exacte avec l'utilisateur** avant de coder |
| 7 | `R1` | Compression des images | À l'upload : vignette de 400 px + version de 1600 px en WebP ; `server.compression.enabled=true` |
| 8 | `R2` | PWA | **Dépôt `kupanga-front`** : service worker Angular, cache hors ligne du dashboard, des quittances et de l'historique |

## Fin de sprint
Lance `testeur` sur la suite complète, puis `revue-securite` sur l'ensemble du sprint. Ajoute au Journal de `.claude/CLAUDE.md` une ligne « Sprint 4 (Congo) terminé ».
