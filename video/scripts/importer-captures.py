"""
Copie les captures de demo/captures/<dossier> dans public/shots (JPEG qualité 93, plus léger à rendre que le PNG)
avec le manifeste (descriptions) et les liens WhatsApp. Vérifie que chaque capture citée par scenario.json existe.

Lancement (depuis video/) : python -I scripts/importer-captures.py [dossier, défaut out/shots]
"""
import json
import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT.parent / "demo" / "captures" / (sys.argv[1] if len(sys.argv) > 1 else "out/shots")
DEST = ROOT / "public" / "shots"
DEST.mkdir(parents=True, exist_ok=True)

for png in sorted(SOURCE.glob("*.png")):
    cible = DEST / (png.stem + ".jpg")
    if cible.exists() and cible.stat().st_mtime >= png.stat().st_mtime:
        continue
    img = Image.open(png).convert("RGB")
    img.save(cible, "JPEG", quality=93, optimize=True)
for nom in ("manifeste.json", "whatsapp.json"):
    if (SOURCE / nom).exists():
        shutil.copy(SOURCE / nom, DEST / nom)

# Dimensions de chaque capture (pour les pages longues qui défilent)
tailles = {p.stem: Image.open(p).size for p in DEST.glob("*.jpg")}
(DEST / "tailles.json").write_text(json.dumps(tailles), encoding="utf-8")

manquantes = set()
for scene in json.loads((ROOT / "scenario.json").read_text(encoding="utf-8"))["scenes"]:
    for plan in scene["plans"]:
        for cle in ("src", "mobile", "fond", "ecran"):
            v = plan.get(cle)
            for nom in (v if isinstance(v, list) else [v] if isinstance(v, str) else []):
                if nom not in tailles:
                    manquantes.add(f"{scene['id']}: {nom}")
print(f"{len(tailles)} captures dans public/shots")
if manquantes:
    print("MANQUANTES :\n  " + "\n  ".join(sorted(manquantes)))
