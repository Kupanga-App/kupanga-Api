# Environnement de démo (vidéo, captures d'écran)

Pile complète **séparée du dev** (autres conteneurs, volumes et ports), remplie avec beaucoup de données
pour montrer la pagination, les tris et les filtres, puis parcourue par un robot (Playwright) qui prend les captures.

| Service | Adresse |
|---|---|
| API | http://localhost:8189 |
| Front (`ng serve -c demo` dans `kupanga-front`) | http://localhost:4300 |
| Mailpit (e-mails reçus) | http://localhost:8125 |
| Back-office | http://localhost:8189/backoffice (`admin@kupanga.demo` / `Admin-Demo-2026`) |

Tous les comptes : mot de passe `Kupanga-Demo-2026` (ex. `claire.martin@kupanga.demo`, propriétaire à Lyon ;
`patrick.mbuyi@kupanga.demo`, propriétaire à Kinshasa ; `lucas.bernard@kupanga.demo`, `grace.mukendi@kupanga.demo`, locataires).

## Tout refaire

```bash
# 0. Une fois : photos CC0 (StockSnap via Openverse) et dépendances
python -I demo/seed/photos.py          # puis trier avec demo/seed/planche.py
pip install boto3 requests pillow bcrypt pymupdf
(cd demo/captures && npm install && npx playwright install chromium)

# 1. Front de démo (autre terminal, dans kupanga-front)
npx ng serve -c demo

# 2. Remise à zéro + remplissage + toutes les captures (4K) + PDF
ECHELLE=2 SORTIE=out/shots bash demo/rejouer.sh

# 3. Images de la landing page (kupanga-front/public/landing) et du portfolio
(cd demo/captures && python -I exporter.py out/shots)
```

- `seed/remplir.py` : ~146 comptes et ~740 biens en SQL (France et RDC, photos, équipements, POI, locations,
  conversations), puis des parcours réels par l'API : biens publiés avec photos, demandes de location, réservation,
  baux et états des lieux signés par les deux parties (liens lus dans Mailpit), quittances payées avec reçu, congé.
- `captures/captures.mjs` : 9 segments rejouables séparément (`node captures.mjs rdc backoffice`) :
  visiteur, inscription, propriétaire France, location complète, fin de bail, RDC, compte, back-office, Mailpit.
  Chaque capture est décrite dans `out/shots/manifeste.json` (légendes de la vidéo).
- `captures/pdf.py` : les vrais PDF (bail, état des lieux, quittance ; France et RDC) rendus en PNG.
- Ne pas modifier le front pendant les captures : le rechargement automatique de `ng serve` les interrompt.
- Remise à zéro seule : `docker compose -p kupanga-demo -f demo/docker-compose-demo.yml down -v`.
