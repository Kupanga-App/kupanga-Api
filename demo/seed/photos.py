"""
Photos de démonstration : photos CC0 (domaine public, sans attribution) de StockSnap, trouvées par l'API
d'Openverse. Candidates rangées par catégorie dans demo/seed/photos/<catégorie>/ ; les photos hors sujet
sont retirées à la main (demo/seed/planche.py) avant le remplissage.

L'API anonyme d'Openverse limite fortement le débit : une recherche par minute (~25 min en tout).
Lancement (depuis la racine du dépôt) : python -I demo/seed/photos.py
"""
import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

DEST = Path(__file__).resolve().parent / "photos"
UA = {"User-Agent": "kupanga-demo-seed/1.0"}
PAUSE = 65

# catégorie -> requêtes (2 pages de 20 résultats chacune)
CATEGORIES = {
    "salon": ["living room", "sofa interior"],
    "cuisine": ["kitchen"],
    "chambre": ["bedroom"],
    "salle-de-bain": ["bathroom"],
    "immeuble": ["apartment building", "building facade"],
    "maison": ["house", "home exterior"],
    "villa": ["villa", "pool", "tropical house"],
    "commerce": ["shop front", "warehouse"],
    "terrain": ["field land", "garden"],
}


def chercher(q: str, page: int):
    url = "https://api.openverse.org/v1/images/?" + urllib.parse.urlencode(
        {"q": q, "source": "stocksnap", "license": "cc0", "aspect_ratio": "wide", "page_size": 20, "page": page})
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return json.load(r)["results"]


def main():
    credits_f = DEST / "credits.json"
    DEST.mkdir(parents=True, exist_ok=True)
    credits = json.loads(credits_f.read_text(encoding="utf-8")) if credits_f.exists() else []
    for cat, requetes in CATEGORIES.items():
        dossier = DEST / cat
        dossier.mkdir(parents=True, exist_ok=True)
        for q in requetes:
            for page in (1, 2):
                try:
                    resultats = chercher(q, page)
                except Exception as e:  # noqa: BLE001
                    print("recherche", q, page, e, file=sys.stderr, flush=True)
                    time.sleep(PAUSE * 3)
                    continue
                for r in resultats:
                    cible = dossier / f"{r['id']}.jpg"
                    if cible.exists() or (r.get("width") or 0) < 1600:
                        continue
                    try:
                        with urllib.request.urlopen(urllib.request.Request(r["url"], headers=UA), timeout=60) as img:
                            cible.write_bytes(img.read())
                        credits.append({"categorie": cat, "fichier": cible.name, "titre": r.get("title"),
                                        "licence": r["license"], "source": r.get("foreign_landing_url")})
                    except Exception as e:  # noqa: BLE001
                        print("téléchargement", r["url"], e, file=sys.stderr, flush=True)
                credits_f.write_text(json.dumps(credits, ensure_ascii=False, indent=1), encoding="utf-8")
                print(cat, q, page, len(list(dossier.glob("*.jpg"))), flush=True)
                time.sleep(PAUSE)


if __name__ == "__main__":
    main()
