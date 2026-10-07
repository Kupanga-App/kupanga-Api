---
name: sprint-3-juridiction
description: Sprint 3 Kupanga — socle multi-juridiction (J1 à J6 : enum Pays, profils YAML, RegleJuridiction, devise + BigDecimal, validation par pays, modèles de documents par pays, config exposée au front). Argument optionnel : un ID (ex. J1) ou "tout".
argument-hint: "[ID | tout]"
disable-model-invocation: true
---

# Sprint 3 — Socle multi-juridiction

Argument reçu : `$ARGUMENTS`

1. Lis `.claude/CLAUDE.md`, **en particulier §4bis** (architecture validée) et §6 (décisions). Suis ensuite exactement `.claude/sprint-workflow.md` pour chaque tâche.
2. Vérifie que les sprints 1 et 2 sont terminés dans `.claude/CLAUDE.md`. Sinon, signale-le et demande confirmation avant de continuer.
3. On démarre avec **FR + CD** seulement. Pas de `if (pays == ...)` dans les services : tout passe par `JuridictionRegistry` et `JuridictionProperties`.

## Ordre des tâches et points d'attention

| Ordre | ID | Tâche | Points d'attention |
|---|---|---|---|
| 1 | `J1` | Enum `Pays` (ISO 3166 : `FR`, `BE`, `CD`, `CG`) | Migration Flyway qui convertit le texte libre existant de `bien.pays` (« France », « RDC »…) en codes. **Lister d'abord les valeurs distinctes présentes en base** (ou demander à l'utilisateur) avant d'écrire la correspondance. Adapter les DTO, specifications et le back-office |
| 2 | `J2` | `JuridictionProperties` + `RegleJuridiction` + `JuridictionRegistry` | YAML `kupanga.juridictions.FR/CD` (`locale`, `fuseau`, `devises`, `devise-defaut`, `champs-obligatoires`, `champs-masques`, `types-bien`, `modele-documents`, `notification`). Validation au démarrage : chaque `Pays` utilisé a un profil et une `RegleJuridiction`. Tests unitaires du registry |
| 3 | `J3` | Devise + `BigDecimal` + valeurs figées | `Bien.devise` ; montants en `BigDecimal` / `NUMERIC(12,2)` (B13) dans les entités, DTO, mappers et PDF ; `Contrat` et `Quittance` copient `pays`, `devise`, `modeleVersion` à leur création. Interdire le changement de pays d'un bien qui a des contrats. Plafonds par devise (C5) : **demander les valeurs** à l'utilisateur |
| 4 | `J4` | Champs d'adresse congolais + `@ValideSelonJuridiction` | Nouveaux champs facultatifs `commune`, `quartier`, `avenue`, `numeroParcelle`, `pointDeRepere` ; `codePostal` et DPE facultatifs dans le DTO, contrôlés par le profil (C1, C3). Contrainte de classe qui applique « obligatoire / masqué / plafond ». Tests FR et CD |
| 5 | `J5` | Modèles de documents par pays | `templates/documents/<pays>/contrat.html`, `quittance.html`, `etat-des-lieux.html`, fragments communs ; formatage `NumberFormat.getCurrencyInstance(locale)`. Le modèle **FR** reprend l'existant. Le modèle **CD** : créer la structure avec un texte provisoire **clairement marqué « À FAIRE VALIDER PAR UN JURISTE »** ; ne jamais inventer de clauses légales présentées comme définitives (C9) |
| 6 | `J6` | `GET /juridictions/{pays}` + formulaire dynamique | Endpoint public en lecture (champs visibles/obligatoires, devises, types de bien). **Dépôt `kupanga-front`** : le formulaire de bien s'adapte au pays choisi ; le back revalide toujours |

## Fin de sprint
Lance `testeur` sur la suite complète, puis `revue-securite` sur l'ensemble du sprint. Ajoute au Journal de `.claude/CLAUDE.md` une ligne « Sprint 3 (juridiction) terminé ». Rappelle à l'utilisateur que le bail RDC doit être validé par un juriste avant la mise en ligne au Congo.
