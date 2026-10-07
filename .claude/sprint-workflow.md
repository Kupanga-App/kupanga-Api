# Déroulé commun des sprints Kupanga

Ce fichier est lu par toutes les skills `/sprint-*`. Il décrit comment traiter **une tâche** du plan.

## Règles absolues
- **Ne jamais committer ni pousser** (`git commit`, `git push`, `--amend`…). L'utilisateur fait tous les commits. Ne pas non plus lancer `git reset`, `git checkout -- …`, `git stash` ni `git clean` sur ses changements.
- **Respecter les décisions de `.claude/CLAUDE.md`** (§6) et l'architecture §4bis. Si une tâche de l'audit contredit une décision, la décision gagne : signale l'écart, ne l'applique pas.
- **Rester dans le périmètre de la tâche.** Si tu vois un autre problème, note-le dans le rapport final, ne le corrige pas.
- **Ne pas deviner un choix qui appartient à l'utilisateur** (valeur métier, plafond, fournisseur, texte juridique) : pose la question avec `AskUserQuestion`.
- Écrire du code qui ressemble au code existant (packages, nommage, Lombok, MapStruct, style des tests Mockito et WebMvc).
- Toute évolution de schéma = **nouvelle** migration Flyway `V{n+1}__description.sql`. Ne jamais modifier une migration existante.

## Choix de la tâche (argument de la skill)
- `<ID>` (ex. `P0-3`) : traiter cette tâche seulement.
- aucun argument : traiter la **première case non cochée** de ce sprint dans `.claude/CLAUDE.md` §5, en suivant l'ordre donné par la skill.
- `tout` : enchaîner les tâches du sprint dans l'ordre, en appliquant la boucle complète à chacune. Arrêter au premier blocage (tests rouges après 3 essais, décision à prendre, action manuelle).

## Boucle pour une tâche

0. **État du dépôt** : lance `git status --short`. S'il y a des changements non commités **qui ne viennent pas de cette tâche**, liste-les et demande à l'utilisateur s'il veut les committer d'abord (pour garder un commit par tâche) ou continuer quand même.
1. **Comprendre** : relis l'item dans `AUDIT-PRODUCTION.md` (et §4bis de `.claude/CLAUDE.md` pour les tâches J*), puis le code concerné. Cherche **tous** les endroits touchés (Grep) : contrôleurs, services, mappers, WebSocket, back-office.
2. **Tâche manuelle ou décision ?** Si la tâche demande une action que seul l'utilisateur peut faire, ou un choix qui lui revient, arrête-toi et explique précisément quoi faire. Exemples : changer des secrets, réécrire l'historique git, faire valider un texte juridique.
3. **Implémenter** le correctif minimal et complet.
4. **Tests** : ajoute ou adapte les tests. Il faut au moins un test qui **échouerait si le problème revenait**. Pour la sécurité : 401 sans token, 403 sur la ressource d'autrui, et aucun champ sensible dans la réponse.
5. **Lancer l'agent `testeur`** (sur les tests ciblés, puis la suite complète à la fin de la tâche).
   - ROUGE → corrige et relance (3 essais au plus, ensuite arrête-toi et fais le point).
   - BLOQUÉ (Docker arrêté) → demande à l'utilisateur de lancer Docker, puis
     `docker compose -f docker-compose-dev.yml --profile test up -d db-test`.
6. **Lancer l'agent `revue-securite`** avec l'ID de la tâche et la liste des fichiers modifiés.
   - Corrige tout ce qui est `BLOQUANT` ou `IMPORTANT` ; les `MINEUR` vont dans le rapport.
   - Après une correction, relance `testeur`. Pas plus de 2 tours de revue.
7. **Mettre à jour `.claude/CLAUDE.md`** :
   - §5 : passer la case en `[x]` et ajouter `(AAAA-MM-JJ)`.
   - §8 Journal : `AAAA-MM-JJ — <ID> : <ce qui a changé> (fichiers principaux) — non commité`.
   - §6 Décisions et §7 Points ouverts si un choix a été fait ou s'est posé pendant la tâche.
8. **Rapport final**, court, en français :
   - ce qui a changé (fichiers) ;
   - résultat des tests (VERT, nombre de tests) ;
   - verdict de la revue + points mineurs restants ;
   - **impacts sur le front `kupanga-front`** (contrat d'API modifié, nouveau statut HTTP, champ retiré…) ;
   - actions manuelles éventuelles pour l'utilisateur ;
   - **message de commit suggéré** (court, en français). Ne pas committer.

## Front `kupanga-front`
Le front est dans un autre dépôt. Si une tâche demande de le modifier (W11, W12, W13, J6, R2…) et que son chemin n'est pas noté dans `.claude/CLAUDE.md` §2, demande-le à l'utilisateur, puis note-le dans `.claude/CLAUDE.md`. Dans le front, lance au moins `npx ng build` (et `npx ng test --watch=false` si des tests existent).
