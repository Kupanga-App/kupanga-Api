"""
Exporte les images tirées des captures de démo vers :
- la landing page du front (kupanga-front/public/landing) ;
- le portfolio (portfolio/images/kupanga), une image par fonctionnalité, en JPEG allégé.

Lancement (depuis demo/captures) : python -I exporter.py [dossier des captures, défaut out/shots]
"""
import shutil
import sys
from pathlib import Path

from PIL import Image

ICI = Path(__file__).resolve().parent
SHOTS = ICI / (sys.argv[1] if len(sys.argv) > 1 else "out/shots")
API = ICI.parent.parent
FRONT = Path("C:/Users/Leclerc/Workflow/FrontEnd/Angular/kupanga-front/public")
PORTFOLIO = Path("C:/Users/Leclerc/Workflow/FrontEnd/portfolio/images")
PHOTOS = ICI.parent / "seed" / "photos"


def jpeg(source: Path, cible: Path, largeur: int, qualite: int = 84) -> None:
    img = Image.open(source).convert("RGB")
    if img.width > largeur:
        img = img.resize((largeur, round(img.height * largeur / img.width)), Image.LANCZOS)
    cible.parent.mkdir(parents=True, exist_ok=True)
    img.save(cible, "JPEG", quality=qualite, optimize=True, progressive=True)


def photo(categorie: str, rang: int) -> Path:
    return sorted((PHOTOS / categorie).glob("*.jpg"))[rang]


# Landing page
L = FRONT / "landing"
jpeg(SHOTS / "01-explorer.png", L / "app-explorer.jpg", 1600)
jpeg(SHOTS / "04-m-mon-logement.png", L / "app-telephone.jpg", 780)
jpeg(photo("salon", 13), L / "france.jpg", 1200)
jpeg(photo("villa", 2), L / "rdc.jpg", 1200)
affiche = API / "video" / "out" / "affiche.png"
jpeg(affiche if affiche.exists() else SHOTS / "01-explorer.png", L / "video-affiche.jpg", 1600)

# Portfolio : (fichier, capture, largeur)
PORT = [
    ("explorer", "01-explorer", 1600), ("carte", "01-carte", 1600), ("annonce-rdc", "01-annonce-rdc", 1600),
    ("accueil", "03-accueil", 1600), ("nouveau-bien", "03-nouveau-caracteristiques", 1600),
    ("bien-loue", "03-bien-loue", 1600),
    ("messagerie", "04-conversation", 1600), ("proposer", "04-demande-envoyee", 1600),
    ("reservation", "04-reservation", 1600), ("bail-signature", "04-bail-signature-proprio", 1600),
    ("m-bail-signature", "04-m-bail-signature", 700), ("edl", "04-edl-4", 1600),
    ("loyer-recu", "04-loyer-recu", 1600), ("m-mon-logement", "04-m-mon-logement", 700),
    ("fin-de-bail", "05-fin-de-bail", 1600),
    ("rdc-adresse", "06-nouveau-adresse", 1600), ("rdc-gps", "06-nouveau-gps", 1600),
    ("rdc-equipements", "06-nouveau-equipements", 1600), ("rdc-quittances", "06-quittances-rdc", 1600),
    ("m-rdc-logement", "06-m-mon-logement", 700),
    ("pdf-bail", "10-pdf-bail-fr-p1", 1100), ("pdf-edl-rdc", "10-pdf-edl-rdc-p1", 1100),
    ("pdf-quittance-rdc", "10-pdf-quittance-rdc-p1", 1100),
    ("email-bail", "04-email-bail", 1600), ("mailpit", "09-mailpit", 1600),
    ("sombre", "07-sombre-accueil", 1600),
    ("bo-tableau-de-bord", "08-bo-tableau-de-bord", 1600), ("bo-biens", "08-bo-biens-filtres", 1600),
    ("bo-emails", "08-bo-emails", 1600),
    # Ajouts : plus de fonctionnalités
    ("annonce-fr", "01-annonce-fr", 1600), ("filtres", "01-filtres", 1600), ("explorer-rdc", "01-explorer-rdc", 1600),
    ("m-explorer", "01-m-explorer", 700), ("inscription", "02-inscription-2", 1600),
    ("email-confirmation", "02-email-confirmation", 1600), ("mes-biens", "03-mes-biens", 1600),
    ("nouveau-carte", "03-nouveau-carte", 1600), ("nouveau-photos", "03-nouveau-photos", 1600),
    ("bien-publie", "03-bien-publie", 1600), ("documents", "03-documents", 1600), ("notifications", "03-notifications", 1600),
    ("demande-recue", "04-demande-recue", 1600), ("bien-reserve", "04-bien-reserve", 1600),
    ("bail-nouveau", "04-bail-2", 1600), ("edl-compteurs", "04-edl-2", 1600), ("quittance-payee", "04-quittance-payee", 1600),
    ("m-quittances", "04-m-quittances", 700), ("m-edl-signature", "04-m-edl-a-signer", 700),
    ("donner-conge", "05-donner-conge", 1600), ("bien-libere", "05-bien-libere", 1600),
    ("rdc-type", "06-nouveau-type", 1600), ("rdc-loyer", "06-nouveau-loyer", 1600), ("rdc-annonce", "06-annonce-publiee", 1600),
    ("rdc-villa", "06-villa-louee", 1600), ("rdc-whatsapp", "06-whatsapp-rappel", 1600),
    ("pdf-bail-rdc", "10-pdf-bail-rdc-p1", 1100), ("pdf-edl-fr", "10-pdf-edl-fr-p2", 1100),
    ("pdf-quittance-fr", "10-pdf-quittance-fr-p1", 1100), ("email-quittance", "04-email-quittance", 1600),
    ("sombre-explorer", "07-sombre-explorer", 1600), ("m-sombre", "07-m-sombre-logement", 700),
    ("bo-login", "08-bo-login", 1600), ("bo-comptes", "08-bo-comptes", 1600), ("bo-archiver", "08-bo-archiver", 1600),
    ("bo-documents", "08-bo-documents", 1600),
]
# Graphiques du back-office (camemberts et barres), découpés dans la page « Analytiques » en 4K
STATS = SHOTS / "08-bo-statistiques-complet.png"
if STATS.exists():
    im = Image.open(STATS).convert("RGB")
    k = im.width / 900  # coordonnées relevées sur une réduction à 900 px de large
    for nom, (x0, y0, x1, y1) in {
        "bo-camembert-comptes-villes": (118, 172, 892, 437), "bo-camembert-types": (118, 436, 892, 772),
        "bo-camembert-documents": (118, 772, 892, 952), "bo-statistiques": (112, 28, 900, 440),
    }.items():
        c = im.crop((round(x0 * k), round(y0 * k), round(x1 * k), round(y1 * k)))
        c.thumbnail((1600, 2000), Image.LANCZOS)
        (PORTFOLIO / "kupanga").mkdir(parents=True, exist_ok=True)
        c.save(PORTFOLIO / "kupanga" / f"{nom}.jpg", "JPEG", quality=86, optimize=True, progressive=True)
for nom, capture, largeur in PORT:
    src = SHOTS / f"{capture}.png"
    if src.exists():
        jpeg(src, PORTFOLIO / "kupanga" / f"{nom}.jpg", largeur, 82)
    else:
        print("absente :", capture)
shutil.copy(FRONT / "brand" / "kupanga-logo.svg", PORTFOLIO / "kupanga-mark.svg")
print("exporté :", L, PORTFOLIO / "kupanga")
