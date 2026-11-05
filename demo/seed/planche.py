"""Planche contact numérotée d'une catégorie de photos (tri manuel). python -I demo/seed/planche.py <catégorie> <sortie.jpg>"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

dossier = Path(__file__).resolve().parent / "photos" / sys.argv[1]
fichiers = sorted(dossier.glob("*.jpg"))
cols, w, h = 8, 240, 160
planche = Image.new("RGB", (cols * w, ((len(fichiers) + cols - 1) // cols) * h), "white")
d = ImageDraw.Draw(planche)
for i, f in enumerate(fichiers):
    try:
        img = ImageOps.fit(Image.open(f).convert("RGB"), (w - 4, h - 4))
    except Exception:  # noqa: BLE001
        continue
    x, y = (i % cols) * w, (i // cols) * h
    planche.paste(img, (x + 2, y + 2))
    d.rectangle((x + 2, y + 2, x + 34, y + 26), fill="black")
    d.text((x + 6, y + 4), str(i), fill="yellow", font_size=18)
planche.save(sys.argv[2], quality=80)
print("\n".join(f"{i} {f.name}" for i, f in enumerate(fichiers)))
