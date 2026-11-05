"""
Remplit l'environnement de DÉMO (demo/docker-compose-demo.yml) — jamais la base de dev.

1. Photos : réduites (1600 px + vignette 400 px, comme l'API) et déposées dans le stockage S3 de démo.
2. Volume (SQL) : ~100 comptes, ~700 biens en France et en RDC, photos, équipements, POI, locations,
   conversations et messages, étalés sur 10 mois (pagination, tris, statistiques du back-office).
3. Parcours réels (API) : biens publiés avec photos, demandes de location, réservations, baux et états
   des lieux signés par les deux parties (liens lus dans Mailpit), quittances payées avec reçu, congé.

Mot de passe de tous les comptes : Kupanga-Demo-2026   ·   Back-office : admin@kupanga.demo / Admin-Demo-2026

Lancement (depuis la racine du dépôt, pile de démo démarrée) : python -I demo/seed/remplir.py
"""
from __future__ import annotations

import base64
import io
import json
import math
import random
import re
import subprocess
import sys
import time
import uuid
from datetime import date, datetime, timedelta
from pathlib import Path

import boto3
import requests
from PIL import Image, ImageDraw, ImageFilter, ImageOps

ICI = Path(__file__).resolve().parent
PHOTOS = ICI / "photos"
API = "http://localhost:8189"
MAILPIT = "http://localhost:8125"
S3 = dict(endpoint_url="http://localhost:9200", aws_access_key_id="kupanga-dev",
          aws_secret_access_key="kupanga-dev-secret", region_name="us-east-1")
BUCKET_PHOTOS = "bucket-photo-immobilier"
MDP = "Kupanga-Demo-2026"
HASH = "$2b$10$C4aoQn7LX6V1py04zVmzseVYPRj3y2ixMmBTu/.XVjouIhlWUsm8e"  # bcrypt de MDP
AUJOURDHUI = date.today()

rnd = random.Random(2026)


# ─────────────────────────────────────────── outils ───────────────────────────────────────────

def sql(texte: str) -> str:
    r = subprocess.run(["docker", "exec", "-i", "kupanga-demo-db", "psql", "-U", "kupanga", "-d", "kupanga_demo",
                        "-v", "ON_ERROR_STOP=1", "-q", "-t", "-A"],
                       input=texte.encode("utf-8"), capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "replace")[-3000:])
    return r.stdout.decode("utf-8").strip()


def q(v) -> str:
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (date, datetime)):
        return f"'{v.isoformat(sep=' ') if isinstance(v, datetime) else v.isoformat()}'"
    return "'" + str(v).replace("'", "''") + "'"


def vider_limites():
    subprocess.run(["docker", "exec", "kupanga-demo-redis", "redis-cli", "FLUSHALL"], capture_output=True)


def attendre_api():
    for _ in range(120):
        try:
            if requests.get(f"{API}/actuator/health/liveness", timeout=3).ok:
                return
        except requests.RequestException:
            pass
        time.sleep(2)
    sys.exit("API de démo injoignable (http://localhost:8189)")


# ─────────────────────────────────────────── photos ───────────────────────────────────────────

def jpeg(img: Image.Image, cote: int) -> bytes:
    img = img.copy()
    img.thumbnail((cote, cote), Image.LANCZOS)
    out = io.BytesIO()
    img.save(out, "JPEG", quality=82, optimize=True, progressive=False)
    return out.getvalue()


def deposer_photos() -> dict[str, list[tuple[str, str]]]:
    s3 = boto3.client("s3", **S3)
    stock: dict[str, list[tuple[str, str]]] = {}
    for dossier in sorted(p for p in PHOTOS.iterdir() if p.is_dir()) if PHOTOS.exists() else []:
        for f in sorted(dossier.glob("*.jpg")):
            try:
                img = ImageOps.exif_transpose(Image.open(f)).convert("RGB")
            except Exception:  # noqa: BLE001
                continue
            # Original et version en miroir (plus de variété entre les annonces)
            for variante in (img, ImageOps.mirror(img)):
                cle, vignette = f"{uuid.uuid4()}.jpg", f"{uuid.uuid4()}.jpg"
                s3.put_object(Bucket=BUCKET_PHOTOS, Key=cle, Body=jpeg(variante, 1600), ContentType="image/jpeg")
                s3.put_object(Bucket=BUCKET_PHOTOS, Key=vignette, Body=jpeg(variante, 400), ContentType="image/jpeg")
                stock.setdefault(dossier.name, []).append((cle, vignette))
    print("photos déposées :", {k: len(v) for k, v in stock.items()})
    return stock


def photo_brute(categorie: str) -> bytes:
    """Une photo d'origine (pour les envois par l'API)."""
    fichiers = sorted((PHOTOS / categorie).glob("*.jpg")) or sorted(PHOTOS.glob("*/*.jpg"))
    if not fichiers:  # essai sans photos : image unie
        return jpeg(Image.new("RGB", (1600, 1067), (143, 164, 100)), 1600)
    img = ImageOps.exif_transpose(Image.open(rnd.choice(fichiers))).convert("RGB")
    return jpeg(img, 1600)


def signature_png(nom: str) -> str:
    """Signature manuscrite factice : PNG 8 bits (IHDR, IDAT, IEND), accepté par @SignaturePng."""
    w, h = 600, 200
    img = Image.new("RGBA", (w, h), (255, 255, 255, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(nom)
    x, y = 30, 120
    pts = [(x, y)]
    for i in range(70):
        x += r.randint(4, 9)
        y = 110 + int(45 * math.sin(i / 2.3 + r.random())) + r.randint(-6, 6)
        pts.append((x, y))
    d.line(pts, fill=(16, 33, 43, 255), width=4, joint="curve")
    d.line([(40, 165), (x - 20, 150)], fill=(16, 33, 43, 255), width=3)
    out = io.BytesIO()
    img.save(out, "PNG")
    return base64.b64encode(out.getvalue()).decode()


# ─────────────────────────────────────────── données ──────────────────────────────────────────

PRENOMS_FR = ["Camille", "Julien", "Léa", "Thomas", "Manon", "Hugo", "Chloé", "Nicolas", "Emma", "Antoine", "Sarah",
              "Maxime", "Laura", "Alexandre", "Pauline", "Romain", "Marie", "Quentin", "Julie", "Mathieu", "Clara",
              "Baptiste", "Élodie", "Vincent", "Anaïs", "Florian", "Lucie", "Kevin", "Margaux", "Adrien", "Yasmine",
              "Mehdi", "Fatou", "Karim", "Inès", "Paul", "Océane", "Louis", "Justine", "Benoît"]
NOMS_FR = ["Martin", "Bernard", "Dubois", "Durand", "Lefebvre", "Moreau", "Laurent", "Simon", "Michel", "Garcia",
           "David", "Bertrand", "Roux", "Vincent", "Fournier", "Morel", "Girard", "André", "Mercier", "Dupont",
           "Lambert", "Bonnet", "François", "Martinez", "Legrand", "Garnier", "Faure", "Rousseau", "Blanc", "Guérin",
           "Muller", "Henry", "Roussel", "Nicolas", "Perrin", "Morin", "Mathieu", "Clément", "Gauthier", "Diallo"]
PRENOMS_CD = ["Patrick", "Grâce", "Jonathan", "Merveille", "Christian", "Divine", "Fiston", "Gloire", "Héritier",
              "Nathalie", "Junior", "Ruth", "Serge", "Esther", "Trésor", "Rachel", "Dieudonné", "Benedicte", "Cédric",
              "Prisca", "Blaise", "Sandrine", "Arsène", "Josué", "Naomie", "Exaucé", "Gédéon", "Chancelle"]
NOMS_CD = ["Mbuyi", "Mukendi", "Ilunga", "Kabongo", "Tshibangu", "Kasongo", "Lukusa", "Mwamba", "Ngoy", "Kalala",
           "Mutombo", "Banza", "Kanku", "Nsimba", "Makiese", "Lutete", "Mbala", "Matondo", "Nzuzi", "Kiala",
           "Lokwa", "Bokele", "Mpia", "Ekofo", "Tshimanga", "Kapinga", "Nkulu", "Mavungu"]

VILLES_FR = [  # ville, lat, lon, code postal, quartiers, poids, loyer au m²
    ("Paris", 48.8566, 2.3522, ["75011", "75015", "75018", "75012", "75020", "75017"],
     ["Bastille", "Montmartre", "Batignolles", "Nation", "Belleville", "Convention"], 22, 32),
    ("Lyon", 45.7640, 4.8357, ["69001", "69003", "69004", "69006", "69007"],
     ["Croix-Rousse", "Presqu'île", "Part-Dieu", "Brotteaux", "Guillotière"], 14, 16),
    ("Marseille", 43.2965, 5.3698, ["13001", "13006", "13007", "13008"],
     ["Vieux-Port", "Castellane", "Endoume", "Prado"], 10, 14),
    ("Bordeaux", 44.8378, -0.5792, ["33000", "33300", "33800"], ["Chartrons", "Saint-Michel", "Bastide"], 9, 15),
    ("Lille", 50.6292, 3.0573, ["59000", "59800"], ["Vieux-Lille", "Wazemmes", "Vauban"], 8, 15),
    ("Toulouse", 43.6047, 1.4442, ["31000", "31400", "31500"], ["Capitole", "Saint-Cyprien", "Carmes"], 9, 14),
    ("Nantes", 47.2184, -1.5536, ["44000", "44100"], ["Bouffay", "Île de Nantes", "Graslin"], 7, 14),
    ("Nice", 43.7102, 7.2620, ["06000", "06300"], ["Vieux-Nice", "Libération", "Port"], 6, 19),
    ("Strasbourg", 48.5734, 7.7521, ["67000"], ["Petite France", "Krutenau", "Neudorf"], 5, 14),
    ("Montpellier", 43.6108, 3.8767, ["34000"], ["Écusson", "Antigone", "Beaux-Arts"], 5, 15),
    ("Rennes", 48.1173, -1.6778, ["35000"], ["Centre", "Thabor", "Sainte-Anne"], 5, 14),
]
RUES_FR = ["rue de la République", "avenue Jean Jaurès", "rue Victor Hugo", "boulevard Gambetta", "rue Pasteur",
           "rue des Lilas", "rue du Moulin", "place de la Liberté", "rue Nationale", "rue Voltaire", "rue Émile Zola",
           "avenue de la Gare", "rue des Écoles", "rue Saint-Michel", "quai des Célestins", "rue de la Paix",
           "rue Lafayette", "boulevard Carnot", "rue du Marché", "allée des Tilleuls"]

COMMUNES_KIN = [  # commune, lat, lon, quartiers, poids, loyer USD de base
    ("Gombe", -4.3060, 15.3070, ["Golf", "Haut-Commandement", "Socimat", "Batetela"], 12, 1500),
    ("Ngaliema", -4.3550, 15.2550, ["Ma Campagne", "Binza Delvaux", "Mont Fleury", "Joli Parc", "Djelo Binza"], 14, 1100),
    ("Limete", -4.3720, 15.3420, ["Résidentiel", "Industriel", "Kingabwa", "Mososo"], 10, 700),
    ("Lingwala", -4.3200, 15.2950, ["Lokole", "Voix du Peuple"], 5, 450),
    ("Kintambo", -4.3330, 15.2700, ["Jamaïque", "Nganda", "Magasin"], 6, 550),
    ("Bandalungwa", -4.3500, 15.2900, ["Lubudi", "Makelele", "Tshibangu"], 6, 400),
    ("Kalamu", -4.3480, 15.3150, ["Matonge", "Yolo", "Kauka"], 6, 380),
    ("Lemba", -4.4000, 15.3200, ["Righini", "Salongo", "Foire"], 6, 320),
    ("Kasa-Vubu", -4.3400, 15.3050, ["Kasa-Vubu", "Assossa"], 4, 350),
    ("Mont-Ngafula", -4.4300, 15.2600, ["Kimwenza", "Mama Yemo", "Lutendele"], 6, 600),
    ("Barumbu", -4.3150, 15.3200, ["Bon Marché", "Ndolo"], 3, 300),
    ("Matete", -4.3850, 15.3400, ["Banunu", "Mutoto"], 3, 260),
    ("Masina", -4.3900, 15.3900, ["Petro-Congo", "Sans Fil"], 4, 220),
]
AUTRES_RDC = [("Lubumbashi", -11.6609, 27.4794, ["Lubumbashi", "Kampemba"], ["Golf", "Baudouin", "Bel-Air"], 900),
              ("Goma", -1.6792, 29.2228, ["Goma", "Karisimbi"], ["Himbi", "Les Volcans", "Katindo"], 700)]
AVENUES_CD = ["de la Justice", "des Aviateurs", "Tombalbaye", "Colonel Mondjiba", "Kasa-Vubu", "de la Libération",
              "Lukusa", "Haut-Congo", "Pumbu", "Batetela", "des Cliniques", "Kabasele", "Ngaba", "du Commerce",
              "Mutombo Katshi", "Kianza", "Bandundu", "Kalembe-Lembe"]
REPERES = ["En face de l'église catholique", "À côté de la station Total", "Derrière le marché",
           "Près du rond-point Victoire", "Après la pharmacie du coin", "En face de l'école primaire",
           "À 50 m de l'arrêt de bus", "Portail bleu, à côté du salon de coiffure", "Près du terrain de football",
           "Face à la boulangerie"]
ADJECTIFS = ["avec balcon", "avec terrasse", "vue dégagée", "proche transports", "plein sud", "au calme",
             "proche commerces", "avec cave", "avec parking", "refait à neuf"]

EQUIP_FR = ["PARKING", "CLIMATISATION"]
EQUIP_CD = ["EAU_REGIDESO", "FORAGE", "CITERNE", "ELECTRICITE_SNEL", "GROUPE_ELECTROGENE", "PANNEAUX_SOLAIRES",
            "CLOTURE", "GARDIENNAGE", "PARKING", "CLIMATISATION"]


def date_entre(debut_jours: int, fin_jours: int) -> datetime:
    j = rnd.randint(fin_jours, debut_jours)
    return datetime.combine(AUJOURDHUI - timedelta(days=j), datetime.min.time()) + timedelta(
        hours=rnd.randint(7, 21), minutes=rnd.randint(0, 59))


def description_fr(type_bien, pieces, surface, quartier, ville, meuble):
    morceaux = [
        f"Au cœur du quartier {quartier}, {('ce ' + type_bien.lower()) if type_bien != 'MAISON' else 'cette maison'} "
        f"de {surface:g} m² offre {pieces} pièce{'s' if pieces > 1 else ''} bien distribuée{'s' if pieces > 1 else ''}.",
        rnd.choice(["Séjour baigné de lumière, cuisine équipée ouverte.", "Belle hauteur sous plafond, parquet ancien.",
                    "Cuisine séparée entièrement équipée, nombreux rangements.",
                    "Double vitrage récent, chauffage individuel."]),
        rnd.choice([f"Commerces, écoles et transports à pied : tout {ville} à portée de main.",
                    "Immeuble bien entretenu, digicode et gardien.", "Copropriété calme et arborée.",
                    "Cave et local à vélos."]),
        "Loué meublé, prêt à vivre." if meuble else "Loué vide.",
    ]
    return " ".join(morceaux)


def description_cd(type_bien, commune, quartier, eq):
    lib = {"VILLA": "Belle villa", "MAISON": "Maison", "APPARTEMENT": "Appartement", "ANNEXE": "Annexe indépendante",
           "CHAMBRE": "Chambre", "LOCAL_COMMERCIAL": "Local commercial", "ENTREPOT": "Entrepôt",
           "PARCELLE": "Parcelle", "TERRAIN": "Terrain"}[type_bien]
    atouts = []
    if "FORAGE" in eq or "EAU_REGIDESO" in eq:
        atouts.append("eau courante")
    if "GROUPE_ELECTROGENE" in eq or "PANNEAUX_SOLAIRES" in eq:
        atouts.append("courant de secours")
    if "GARDIENNAGE" in eq:
        atouts.append("gardien jour et nuit")
    if "CLOTURE" in eq:
        atouts.append("parcelle clôturée")
    texte = f"{lib} à {commune}, quartier {quartier}."
    if atouts:
        texte += " " + ", ".join(atouts).capitalize() + "."
    texte += " " + rnd.choice(["Accès facile, route asphaltée jusqu'au portail.", "Quartier calme et sécurisé.",
                               "Proche du grand marché et des écoles.", "Idéal pour une famille.",
                               "Visites sur rendez-vous avec le propriétaire."])
    return texte


# ─────────────────────────────────────── comptes ──────────────────────────────────────────

HEROS = [  # id, prénom, nom, e-mail, rôle, téléphone
    (1, "Claire", "Martin", "claire.martin@kupanga.demo", "ROLE_PROPRIETAIRE", "+33612345678"),
    (2, "Patrick", "Mbuyi", "patrick.mbuyi@kupanga.demo", "ROLE_PROPRIETAIRE", "+243812345678"),
    (3, "Lucas", "Bernard", "lucas.bernard@kupanga.demo", "ROLE_LOCATAIRE", "+33687654321"),
    (4, "Inès", "Moreau", "ines.moreau@kupanga.demo", "ROLE_LOCATAIRE", "+33611223344"),
    (5, "Hugo", "Lefèvre", "hugo.lefevre@kupanga.demo", "ROLE_LOCATAIRE", None),
    (6, "Sophie", "Garnier", "sophie.garnier@kupanga.demo", "ROLE_LOCATAIRE", "+33622334455"),
    (7, "Grâce", "Mukendi", "grace.mukendi@kupanga.demo", "ROLE_LOCATAIRE", "+243970001122"),
    (8, "Jonathan", "Ilunga", "jonathan.ilunga@kupanga.demo", "ROLE_LOCATAIRE", "+243990112233"),
    (9, "Nadia", "Benali", "nadia.benali@kupanga.demo", "ROLE_LOCATAIRE", "+33633445566"),
    (10, "Merveille", "Kabongo", "merveille.kabongo@kupanga.demo", "ROLE_LOCATAIRE", "+243815556677"),
]


def generer_comptes():
    comptes = []  # (id, prénom, nom, email, rôle, tel, pays, créé le)
    for h in HEROS:
        comptes.append((*h, "FR" if h[5] is None or h[5].startswith("+33") else "CD", date_entre(300, 240)))
    nid = 11
    vus = {c[3] for c in comptes}

    def ajouter(prenoms, noms, role, pays, n):
        nonlocal nid
        for _ in range(n):
            p, nm = rnd.choice(prenoms), rnd.choice(noms)
            mail = f"{p.lower()}.{nm.lower()}@exemple.fr" if pays == "FR" else f"{p.lower()}.{nm.lower()}@exemple.cd"
            mail = mail.translate(str.maketrans("éèëêïîôçàâ", "eeeeiiocaa"))
            k = 2
            while mail in vus:
                mail = re.sub(r"(\d*)@", f"{k}@", mail, count=1)
                k += 1
            vus.add(mail)
            tel = None
            if rnd.random() < 0.6:
                tel = (f"+336{rnd.randint(10000000, 99999999)}" if pays == "FR"
                       else f"+2438{rnd.randint(10000000, 99999999)}")
            comptes.append((nid, p, nm, mail, role, tel, pays, date_entre(300, 1)))
            nid += 1

    ajouter(PRENOMS_FR, NOMS_FR, "ROLE_PROPRIETAIRE", "FR", 34)
    ajouter(PRENOMS_CD, NOMS_CD, "ROLE_PROPRIETAIRE", "CD", 22)
    ajouter(PRENOMS_FR, NOMS_FR, "ROLE_LOCATAIRE", "FR", 46)
    ajouter(PRENOMS_CD, NOMS_CD, "ROLE_LOCATAIRE", "CD", 34)
    return comptes


# ─────────────────────────────────────────── biens ────────────────────────────────────────────

def photos_pour(stock, cats, n):
    if n == 0:
        return []
    choix = []
    for c in cats:
        if stock.get(c):
            choix.append(rnd.choice(stock[c]))
    reste = [p for c in ("salon", "cuisine", "chambre", "salle-de-bain") for p in stock.get(c, [])]
    while len(choix) < n and reste:
        p = rnd.choice(reste)
        if p not in choix:
            choix.append(p)
    return choix[:n]


def bien_fr(bid, proprio, stock):
    ville, lat, lon, cps, quartiers, _, prix_m2 = rnd.choices(VILLES_FR, weights=[v[5] for v in VILLES_FR])[0]
    t = rnd.choices(["APPARTEMENT", "STUDIO", "MAISON"], weights=[62, 20, 18])[0]
    if t == "STUDIO":
        pieces, surface = 1, rnd.randint(16, 32)
    elif t == "APPARTEMENT":
        pieces = rnd.choices([2, 3, 4, 5], weights=[35, 35, 20, 10])[0]
        surface = rnd.randint(22, 30) * pieces // 2 + rnd.randint(15, 30)
    else:
        pieces = rnd.randint(4, 7)
        surface = rnd.randint(85, 190)
    meuble = t == "STUDIO" and rnd.random() < 0.7 or rnd.random() < 0.15
    quartier = rnd.choice(quartiers)
    loyer = round(surface * prix_m2 * rnd.uniform(0.85, 1.2) / 5) * 5
    charges = round(surface * rnd.uniform(1.2, 2.8) / 5) * 5
    nom_type = {"APPARTEMENT": f"Appartement T{pieces}", "STUDIO": "Studio", "MAISON": f"Maison {pieces} pièces"}[t]
    titre = f"{nom_type} {rnd.choice(ADJECTIFS)}, {quartier}"
    cree = date_entre(300, 0)
    exterieur = "maison" if t == "MAISON" else "immeuble"
    return dict(
        id=bid, titre=titre, adresse=f"{rnd.randint(1, 140)} {rnd.choice(RUES_FR)}", ville=ville,
        code_postal=rnd.choice(cps), pays="FR", devise="EUR",
        description=description_fr(t, pieces, surface, quartier, ville, meuble), type_bien=t,
        lat=lat + rnd.gauss(0, 0.018), lon=lon + rnd.gauss(0, 0.025), precision="ADRESSE",
        created_at=cree, proprietaire_id=proprio, surface=surface, pieces=pieces,
        chambres=max(0, pieces - 1), etage=None if t == "MAISON" else rnd.randint(0, 7),
        ascenseur=t != "MAISON" and rnd.random() < 0.5, annee=rnd.randint(1880, 2022),
        chauffage=rnd.choice(["ELECTRIQUE", "GAZ", "COLLECTIF", "POMPE_A_CHALEUR"]),
        dpe=rnd.choices("ABCDEFG", weights=[4, 10, 25, 30, 18, 9, 4])[0],
        ges=rnd.choices("ABCDEFG", weights=[10, 15, 25, 25, 15, 7, 3])[0],
        loyer=loyer, charges=charges, depot=loyer * (2 if meuble else 1), meuble=meuble,
        colocation=pieces >= 4 and rnd.random() < 0.2,
        dispo=AUJOURDHUI + timedelta(days=rnd.randint(-30, 75)),
        equipements=[e for e in EQUIP_FR if rnd.random() < 0.3],
        photos=photos_pour(stock, [exterieur, "salon", "cuisine", "chambre", "salle-de-bain"], rnd.randint(3, 6)),
        commune=None, quartier=None, avenue=None, parcelle=None, repere=None,
        garantie=None, avance=None, heures=None,
    )


def bien_cd(bid, proprio, stock):
    if rnd.random() < 0.85:
        commune, lat, lon, quartiers, _, base = rnd.choices(COMMUNES_KIN, weights=[c[4] for c in COMMUNES_KIN])[0]
        ville = "Kinshasa"
    else:
        ville, lat, lon, communes, quartiers, base = rnd.choice(AUTRES_RDC)
        commune = rnd.choice(communes)
    t = rnd.choices(["APPARTEMENT", "MAISON", "VILLA", "ANNEXE", "CHAMBRE", "LOCAL_COMMERCIAL", "ENTREPOT",
                     "PARCELLE", "TERRAIN"], weights=[24, 20, 16, 12, 8, 8, 4, 4, 4])[0]
    sans_logement = t in ("PARCELLE", "TERRAIN")
    pieces = None if sans_logement else {"CHAMBRE": 1, "ANNEXE": rnd.randint(2, 3), "APPARTEMENT": rnd.randint(2, 5),
                                         "MAISON": rnd.randint(3, 6), "VILLA": rnd.randint(5, 9),
                                         "LOCAL_COMMERCIAL": rnd.randint(1, 3), "ENTREPOT": 1}[t]
    surface = (rnd.randint(300, 1500) if sans_logement and rnd.random() < 0.7 else None) if sans_logement else {
        "CHAMBRE": rnd.randint(12, 20), "ANNEXE": rnd.randint(30, 60), "APPARTEMENT": rnd.randint(55, 160),
        "MAISON": rnd.randint(90, 220), "VILLA": rnd.randint(220, 600), "LOCAL_COMMERCIAL": rnd.randint(25, 150),
        "ENTREPOT": rnd.randint(200, 1200)}[t]
    facteur = {"CHAMBRE": 0.12, "ANNEXE": 0.35, "APPARTEMENT": 1, "MAISON": 1.2, "VILLA": 2.6,
               "LOCAL_COMMERCIAL": 1.1, "ENTREPOT": 2.2, "PARCELLE": 0.5, "TERRAIN": 0.4}[t]
    loyer_usd = max(40, round(base * facteur * rnd.uniform(0.7, 1.3) / 10) * 10)
    devise = "CDF" if rnd.random() < 0.2 and t in ("CHAMBRE", "ANNEXE", "APPARTEMENT", "MAISON") else "USD"
    loyer = loyer_usd * 2800 if devise == "CDF" else loyer_usd
    if devise == "CDF":
        loyer = round(loyer / 5000) * 5000
    eq = [e for e in EQUIP_CD if rnd.random() < 0.45] if not sans_logement else [e for e in ["CLOTURE"] if rnd.random() < .6]
    if t == "VILLA":
        eq = sorted(set(eq) | {"CLOTURE", "GARDIENNAGE", "FORAGE"})
    quartier = rnd.choice(quartiers)
    precision = rnd.choices(["REPERE", "ADRESSE", "QUARTIER", "COMMUNE"], weights=[40, 25, 25, 10])[0]
    dispersion = {"REPERE": 0.012, "ADRESSE": 0.012, "QUARTIER": 0.008, "COMMUNE": 0.004}[precision]
    nom_type = {"VILLA": f"Villa {max(1, (pieces or 2) - 2)} chambres", "MAISON": f"Maison {pieces} pièces",
                "APPARTEMENT": f"Appartement {pieces} pièces", "ANNEXE": "Annexe meublée" if rnd.random() < .4 else "Annexe",
                "CHAMBRE": "Chambre à louer", "LOCAL_COMMERCIAL": "Local commercial", "ENTREPOT": "Entrepôt",
                "PARCELLE": "Parcelle", "TERRAIN": "Terrain"}[t]
    titre = f"{nom_type} à {quartier} ({commune})" if quartier != commune else f"{nom_type} à {commune}"
    cat_ext = {"VILLA": rnd.choice(["villa", "villa", "maison"]), "MAISON": "maison", "APPARTEMENT": "immeuble",
               "ANNEXE": "maison", "CHAMBRE": "chambre", "LOCAL_COMMERCIAL": "immeuble", "ENTREPOT": "immeuble",
               "PARCELLE": None, "TERRAIN": None}[t]
    cats = [cat_ext] if t in ("LOCAL_COMMERCIAL", "ENTREPOT") else [cat_ext, "salon", "chambre", "cuisine"]
    nb = 0 if sans_logement else 1 if t in ("LOCAL_COMMERCIAL", "ENTREPOT") else rnd.randint(3, 6)
    return dict(
        id=bid, titre=titre, adresse=f"n° {rnd.randint(1, 250)}, avenue {rnd.choice(AVENUES_CD)}", ville=ville,
        code_postal=None, pays="CD", devise=devise, description=description_cd(t, commune, quartier, eq), type_bien=t,
        lat=lat + rnd.gauss(0, dispersion), lon=lon + rnd.gauss(0, dispersion), precision=precision,
        created_at=date_entre(300, 0), proprietaire_id=proprio, surface=surface, pieces=pieces,
        chambres=None if pieces is None else max(0, pieces - 2), etage=None, ascenseur=False, annee=None,
        chauffage=None, dpe=None, ges=None, loyer=loyer, charges=0, depot=None,
        meuble=t in ("ANNEXE", "CHAMBRE") and rnd.random() < 0.4, colocation=False,
        dispo=AUJOURDHUI + timedelta(days=rnd.randint(-20, 60)), equipements=eq,
        photos=photos_pour(stock, cats, nb),
        commune=commune, quartier=quartier, avenue=None, parcelle=f"{rnd.randint(1, 9999)}",
        repere=rnd.choice(REPERES) if rnd.random() < 0.75 else None,
        garantie=rnd.choice([1, 2, 3, 3, 6]), avance=rnd.choice([1, 2, 3, 3, 6, 12]),
        heures=None if sans_logement else rnd.choice([6, 8, 12, 16, 18, 20, 24]),
    )


def sql_biens(biens) -> str:
    lignes = []
    for b in biens:
        lignes.append(
            "INSERT INTO biens (id, titre, adresse, ville, code_postal, pays, description, type_bien, localisation, "
            "created_at, updated_at, proprietaire_id, locataire_id, surface_habitable, nombre_pieces, nombre_chambres, "
            "etage, ascenseur, annee_construction, mode_chauffage, classe_energie, classe_ges, loyer_mensuel, "
            "charges_mensuelles, depot_garantie, meuble, colocation, disponible_de, archive, date_archivage, devise, "
            "commune, quartier, avenue, numero_parcelle, point_de_repere, precision_localisation, garantie_mois, "
            "avance_mois, heures_electricite_par_jour) VALUES ("
            + ", ".join([q(b["id"]), q(b["titre"]), q(b["adresse"]), q(b["ville"]), q(b["code_postal"]), q(b["pays"]),
                         q(b["description"]), q(b["type_bien"]),
                         f"ST_SetSRID(ST_MakePoint({b['lon']:.6f}, {b['lat']:.6f}), 4326)",
                         q(b["created_at"]), q(b["created_at"]), q(b["proprietaire_id"]), q(b.get("locataire_id")),
                         q(b["surface"]), q(b["pieces"]), q(b["chambres"]), q(b["etage"]), q(b["ascenseur"]),
                         q(b["annee"]), q(b["chauffage"]), q(b["dpe"]), q(b["ges"]), q(b["loyer"]), q(b["charges"]),
                         q(b["depot"]), q(b["meuble"]), q(b["colocation"]), q(b["dispo"]), q(b.get("archive", False)),
                         q(b.get("date_archivage")), q(b["devise"]), q(b["commune"]), q(b["quartier"]), q(b["avenue"]),
                         q(b["parcelle"]), q(b["repere"]), q(b["precision"]), q(b["garantie"]), q(b["avance"]),
                         q(b["heures"])]) + ");")
        for cle, vignette in b["photos"]:
            lignes.append(f"INSERT INTO bien_images (bien_id, cle, cle_vignette) VALUES ({b['id']}, {q(cle)}, {q(vignette)});")
        for e in b["equipements"]:
            lignes.append(f"INSERT INTO bien_equipements (bien_id, equipement) VALUES ({b['id']}, {q(e)});")
        if b["precision"] in ("REPERE", "ADRESSE"):
            for poi, p in (("SCHOOL", .9), ("HOSPITAL", .6), ("PHARMACY", .85), ("KINDERGARTEN", .7)):
                n = rnd.randint(1, 14) if rnd.random() < p else 0
                lignes.append("INSERT INTO bien_poi (bien_id, poi_type, present, rayon_metres, nombre_trouve) "
                              f"VALUES ({b['id']}, '{poi}', {q(n > 0)}, 5000, {n});")
    return "\n".join(lignes)


# ─────────────────────────────────────────── volume ───────────────────────────────────────────

def remplir_volume(stock):
    comptes = generer_comptes()
    proprios_fr = [c for c in comptes if c[4] == "ROLE_PROPRIETAIRE" and c[6] == "FR" and c[0] > 10]
    proprios_cd = [c for c in comptes if c[4] == "ROLE_PROPRIETAIRE" and c[6] == "CD" and c[0] > 10]
    locs_fr = [c for c in comptes if c[4] == "ROLE_LOCATAIRE" and c[6] == "FR" and c[0] > 10]
    locs_cd = [c for c in comptes if c[4] == "ROLE_LOCATAIRE" and c[6] == "CD" and c[0] > 10]

    s = ["BEGIN;"]
    for (i, p, n, m, role, tel, _pays, cree) in comptes:
        s.append("INSERT INTO utilisateurs (id, prenom, nom, email, mot_de_passe, role, a_completer_profil, "
                 f"email_verifie, telephone) VALUES ({i}, {q(p)}, {q(n)}, {q(m)}, {q(HASH)}, {q(role)}, true, true, {q(tel)});")
    # quelques comptes non confirmés (back-office)
    s.append(f"UPDATE utilisateurs SET email_verifie = false WHERE id IN ({', '.join(str(c[0]) for c in locs_fr[-3:])});")

    biens, bid = [], 1
    # Claire (1) et Patrick (2) : beaucoup de biens pour la pagination de « Mes biens »
    for _ in range(22):
        b = bien_fr(bid, 1, stock); b["ville"] = "Lyon"; biens.append(b); bid += 1
    for _ in range(16):
        b = bien_cd(bid, 2, stock); biens.append(b); bid += 1
    for _ in range(430):
        biens.append(bien_fr(bid, rnd.choice(proprios_fr)[0], stock)); bid += 1
    for _ in range(270):
        biens.append(bien_cd(bid, rnd.choice(proprios_cd)[0], stock)); bid += 1

    # Corrige la ville des biens de Claire (coordonnées de Lyon)
    for b in biens[:22]:
        b["lat"], b["lon"] = 45.7640 + rnd.gauss(0, 0.015), 4.8357 + rnd.gauss(0, 0.02)
        b["code_postal"] = rnd.choice(["69001", "69003", "69004", "69006", "69007"])
        b["titre"] = re.sub(r", .*$", ", " + rnd.choice(["Croix-Rousse", "Presqu'île", "Part-Dieu", "Brotteaux", "Guillotière"]), b["titre"])

    # 18 % loués (location active), 6 archivés
    locations = []
    for b in biens:
        if b["proprietaire_id"] in (1, 2):
            continue  # biens de Claire et Patrick : loués seulement par les parcours réels (baux, quittances)
        if rnd.random() < 0.18:
            loc = rnd.choice(locs_fr if b["pays"] == "FR" else locs_cd)
            b["locataire_id"] = loc[0]
            debut = b["created_at"] + timedelta(days=rnd.randint(10, 40))
            if debut.date() > AUJOURDHUI:
                debut = datetime.combine(AUJOURDHUI - timedelta(days=3), datetime.min.time())
            locations.append((b["id"], b["proprietaire_id"], loc[0], debut))
    for b in rnd.sample([b for b in biens if b["id"] > 40 and not b.get("locataire_id")], 6):
        b["archive"], b["date_archivage"] = True, date_entre(60, 2)

    s.append(sql_biens(biens))
    for (bien, proprio, loc, debut) in locations:
        s.append("INSERT INTO locations (bien_id, proprietaire_id, locataire_id, statut, date_reservation, "
                 f"date_expiration, date_debut) VALUES ({bien}, {proprio}, {loc}, 'ACTIVE', {q(debut - timedelta(days=5))}, "
                 f"{q(debut + timedelta(days=9))}, {q(debut)});")

    # Conversations et messages (candidats ↔ propriétaires)
    mails = {c[0]: c[3] for c in comptes}
    phrases_q = ["Bonjour, le logement est-il toujours disponible ?", "Bonjour, je suis intéressé(e) par votre annonce.",
                 "Serait-il possible de visiter cette semaine ?", "Les charges comprennent-elles l'eau ?",
                 "Mbote ! Le bien est-il encore libre ?", "Bonjour, y a-t-il un parking ?",
                 "Est-ce que le groupe électrogène est inclus ?", "Je peux passer samedi matin ?"]
    phrases_r = ["Bonjour, oui il est toujours disponible.", "Avec plaisir, quel créneau vous arrange ?",
                 "Oui, l'eau est comprise dans les charges.", "Mbote ! Oui, visite possible demain à 10 h.",
                 "Je vous envoie l'adresse exacte en privé.", "Samedi 10 h, c'est parfait."]
    cid = 1
    for b in rnd.sample([b for b in biens if b["id"] > 40 and not b.get("archive")], 160):
        cand = rnd.choice(locs_fr if b["pays"] == "FR" else locs_cd)
        if cand[0] == b.get("locataire_id"):
            continue
        quand = b["created_at"] + timedelta(days=rnd.randint(1, 20))
        if quand > datetime.now():
            quand = datetime.now() - timedelta(hours=rnd.randint(2, 48))
        echanges = [(cand[0], b["proprietaire_id"], rnd.choice(phrases_q))]
        if rnd.random() < 0.7:
            echanges.append((b["proprietaire_id"], cand[0], rnd.choice(phrases_r)))
        if rnd.random() < 0.4:
            echanges.append((cand[0], b["proprietaire_id"], "Merci beaucoup, à bientôt !"))
        s.append(f"INSERT INTO conversations (id, bien_id, last_message, last_message_at, created_at, email_expediteur, "
                 f"email_destinataire) VALUES ({cid}, {b['id']}, {q(echanges[-1][2])}, "
                 f"{q(quand + timedelta(hours=len(echanges)))}, {q(quand)}, {q(mails[cand[0]])}, {q(mails[b['proprietaire_id']])});")
        for k, (de, a, txt) in enumerate(echanges):
            s.append("INSERT INTO messages (contenu, created_at, lu, expediteur_id, destinataire_id, conversation_id) "
                     f"VALUES ({q(txt)}, {q(quand + timedelta(hours=k))}, true, {de}, {a}, {cid});")
        cid += 1

    for table, col in (("utilisateurs", "id"), ("biens", "id"), ("conversations", "id")):
        s.append(f"SELECT setval(pg_get_serial_sequence('{table}', '{col}'), (SELECT max({col}) FROM {table}));")
    s.append("COMMIT;")
    sql("\n".join(s))
    print(f"volume : {len(comptes)} comptes, {len(biens)} biens, {len(locations)} locations, {cid - 1} conversations")
    return mails, cid


# ─────────────────────────────────────── parcours par l'API ───────────────────────────────────

class Compte:
    def __init__(self, email):
        self.email = email
        self.s = requests.Session()
        vider_limites()
        r = self.s.post(f"{API}/auth/login", json={"email": email, "password": MDP}, timeout=30)
        r.raise_for_status()
        self.jwt = r.json().get("accessToken") or r.json().get("token")
        self.s.headers["Authorization"] = f"Bearer {self.jwt}"

    def req(self, methode, chemin, **kw):
        r = self.s.request(methode, API + chemin, timeout=120, **kw)
        if r.status_code == 401:  # jeton de 5 min expiré : nouvelle connexion
            self.__init__(self.email)
            r = self.s.request(methode, API + chemin, timeout=120, **kw)
        if not r.ok:
            raise RuntimeError(f"{methode} {chemin} → {r.status_code} {r.text[:400]}")
        return r.json() if r.content and "json" in r.headers.get("content-type", "") else None


def creer_bien(compte: Compte, form: dict, categories: list[str]) -> int:
    fichiers = [("files", (f"photo{i}.jpg", photo_brute(c), "image/jpeg")) for i, c in enumerate(categories)]
    fichiers.insert(0, ("bienFormDTO", (None, json.dumps(form, ensure_ascii=False), "application/json")))
    compte.req("POST", "/biens", files=fichiers)
    return int(sql(f"select max(id) from biens where titre = {q(form['titre'])}"))


def conversation(bien_id: int, de: str, a: str, echanges: list[tuple[str, str]], il_y_a_heures=30):
    """Conversation candidat → propriétaire (nécessaire pour proposer la location)."""
    ids = dict(x.split("|") for x in sql(f"select email || '|' || id from utilisateurs where email in ({q(de)}, {q(a)})").splitlines())
    debut = datetime.now() - timedelta(hours=il_y_a_heures)
    cid = int(sql(f"INSERT INTO conversations (bien_id, last_message, last_message_at, created_at, email_expediteur, "
                  f"email_destinataire) VALUES ({bien_id}, {q(echanges[-1][1])}, {q(debut + timedelta(minutes=20 * len(echanges)))}, "
                  f"{q(debut)}, {q(de)}, {q(a)}) RETURNING id;").splitlines()[0])
    for k, (qui, txt) in enumerate(echanges):
        exp, dest = (ids[de], ids[a]) if qui == "c" else (ids[a], ids[de])
        sql("INSERT INTO messages (contenu, created_at, lu, expediteur_id, destinataire_id, conversation_id) VALUES "
            f"({q(txt)}, {q(debut + timedelta(minutes=20 * k))}, true, {exp}, {dest}, {cid});")


def jeton_mailpit(destinataire: str, motif: str, deja: set[str]) -> str:
    """Attend l'e-mail (file d'envoi, au plus ~40 s) et extrait le jeton du lien."""
    for _ in range(60):
        r = requests.get(f"{MAILPIT}/api/v1/search", params={"query": f"to:{destinataire}", "limit": 50}, timeout=10).json()
        for m in r.get("messages", []):
            if m["ID"] in deja:
                continue
            html = requests.get(f"{MAILPIT}/api/v1/message/{m['ID']}", timeout=10).json().get("HTML", "")
            t = re.search(motif + r"([A-Za-z0-9\-_]+)", html)
            if t:
                deja.add(m["ID"])
                return t.group(1)
        time.sleep(2)
    raise RuntimeError(f"aucun e-mail « {motif} » pour {destinataire}")


def uid(email):
    return int(sql(f"select id from utilisateurs where email = {q(email)}"))


def louer(proprio: Compte, loc: Compte, bien_id: int, bail: dict, edl: dict | None, deja: set[str]):
    """Demande → acceptation → bail signé par les deux → état des lieux d'entrée signé."""
    d = proprio.req("POST", f"/biens/{bien_id}/demandes-location/{uid(loc.email)}")
    loc.req("POST", f"/demandes-location/{d['id']}/accepter")
    proprio.req("POST", "/contrats", json={"bienId": bien_id, "emailLocataire": loc.email, **bail})
    cid = int(sql(f"select max(id) from contrats where bien_id = {bien_id}"))
    proprio.req("POST", f"/contrats/{cid}/signer-proprio", json={"signatureBase64": signature_png(proprio.email)})
    jeton = jeton_mailpit(loc.email, r"signer/", deja)
    requests.post(f"{API}/contrats/signer/{jeton}", json={"signatureBase64": signature_png(loc.email)},
                  timeout=60).raise_for_status()
    if edl:
        proprio.req("POST", "/etats-des-lieux", json={"bienId": bien_id, "emailLocataire": loc.email, **edl})
        eid = int(sql(f"select max(id) from etats_des_lieux where bien_id = {bien_id}"))
        proprio.req("POST", f"/etats-des-lieux/{eid}/signer-proprietaire",
                    json={"signatureBase64": signature_png(proprio.email)})
        jeton = jeton_mailpit(loc.email, r"signer/", deja)
        requests.post(f"{API}/etats-des-lieux/signer/{jeton}", json={"signatureBase64": signature_png(loc.email)},
                      timeout=60).raise_for_status()
    return cid


def quittance(proprio: Compte, bien_id: int, email_loc: str, contrat: int, mois: int, annee: int,
              payee: dict | None, recu: str | None = None):
    proprio.req("POST", "/quittances", json={"bienId": bien_id, "emailLocataire": email_loc, "contratId": contrat,
                                             "mois": str(mois), "annee": annee,
                                             "dateEcheance": date(annee, mois, 5).isoformat()})
    qid = int(sql(f"select max(id) from quittances where bien_id = {bien_id}"))
    if payee:
        proprio.req("POST", f"/quittances/{qid}/marquer-payee",
                    json={"signatureBase64": signature_png(proprio.email), **payee})
        if recu:
            proprio.req("POST", f"/quittances/{qid}/recu",
                        files={"recu": ("recu.jpg", recu_photo(recu), "image/jpeg")})
    return qid


def recu_photo(texte: str) -> bytes:
    img = Image.new("RGB", (1200, 900), (250, 248, 240))
    d = ImageDraw.Draw(img)
    d.rectangle((60, 60, 1140, 840), outline=(40, 40, 40), width=4)
    y = 110
    for ligne in texte.split("\n"):
        d.text((110, y), ligne, fill=(30, 30, 30), font_size=44)
        y += 80
    img = img.rotate(-2, expand=False, fillcolor=(200, 200, 200)).filter(ImageFilter.GaussianBlur(0.6))
    out = io.BytesIO()
    img.save(out, "JPEG", quality=85)
    return out.getvalue()


EDL_FR = {"type": "ENTREE", "dateRealisation": AUJOURDHUI.isoformat(), "heureRealisation": "10:00:00",
          "observations": "Logement propre, remis en bon état général.",
          "pieces": [
              {"nomPiece": "Entrée", "ordre": 1, "elements": [
                  {"typeElement": "PORTE", "etatElement": "BON", "description": "Porte blindée"},
                  {"typeElement": "SOL", "etatElement": "BON", "description": "Carrelage"}]},
              {"nomPiece": "Séjour", "ordre": 2, "elements": [
                  {"typeElement": "MUR", "etatElement": "BON", "description": "Peinture blanche récente"},
                  {"typeElement": "SOL", "etatElement": "USAGE_NORMAL", "description": "Parquet chêne",
                   "observation": "Légères rayures près de la fenêtre"},
                  {"typeElement": "FENETRE", "etatElement": "BON", "description": "Double vitrage"},
                  {"typeElement": "RADIATEUR", "etatElement": "BON"}]},
              {"nomPiece": "Cuisine", "ordre": 3, "elements": [
                  {"typeElement": "EQUIPEMENT", "etatElement": "BON", "description": "Plaques induction, hotte, four"},
                  {"typeElement": "MUR", "etatElement": "USAGE_NORMAL", "description": "Crédence carrelée"}]},
              {"nomPiece": "Chambre", "ordre": 4, "elements": [
                  {"typeElement": "MUR", "etatElement": "BON"}, {"typeElement": "VOLET", "etatElement": "BON"}]},
              {"nomPiece": "Salle de bain", "ordre": 5, "elements": [
                  {"typeElement": "EQUIPEMENT", "etatElement": "BON", "description": "Douche à l'italienne, vasque"},
                  {"typeElement": "LUMINAIRE", "etatElement": "HORS_SERVICE", "observation": "Ampoule à remplacer"}]}],
          "compteurs": [{"typeCompteur": "ELECTRICITE", "numeroCompteur": "PDL 14 2259 3381 07", "index": 18342, "unite": "kWh"},
                        {"typeCompteur": "EAU_FROIDE", "numeroCompteur": "EF-77120", "index": 412.5, "unite": "m³"}],
          "cles": [{"typeCle": "Porte d'entrée", "quantite": 2}, {"typeCle": "Boîte aux lettres", "quantite": 1},
                   {"typeCle": "Badge immeuble", "quantite": 2}]}

EDL_CD = {"type": "ENTREE", "dateRealisation": AUJOURDHUI.isoformat(), "heureRealisation": "09:30:00",
          "observations": "Villa remise propre. Groupe électrogène révisé le mois dernier.",
          "pieces": [
              {"nomPiece": "Salon", "ordre": 1, "elements": [
                  {"typeElement": "MUR", "etatElement": "BON"}, {"typeElement": "SOL", "etatElement": "BON", "description": "Carrelage"},
                  {"typeElement": "EQUIPEMENT", "etatElement": "BON", "description": "Climatiseur split"}]},
              {"nomPiece": "Cuisine", "ordre": 2, "elements": [
                  {"typeElement": "EQUIPEMENT", "etatElement": "USAGE_NORMAL", "description": "Cuisinière 4 feux"}]},
              {"nomPiece": "Chambre principale", "ordre": 3, "elements": [
                  {"typeElement": "PORTE", "etatElement": "BON"}, {"typeElement": "FENETRE", "etatElement": "BON",
                                                                  "description": "Moustiquaire"}]},
              {"nomPiece": "Cour", "ordre": 4, "elements": [
                  {"typeElement": "AUTRE", "etatElement": "BON", "description": "Portail métallique et clôture"}]}],
          "compteurs": [{"typeCompteur": "CASH_POWER", "numeroCompteur": "CP 0142 7781", "index": 85, "unite": "kWh"},
                        {"typeCompteur": "EAU_FROIDE", "numeroCompteur": "REGIDESO 33-9917", "index": 1290, "unite": "m³"},
                        {"typeCompteur": "GROUPE_ELECTROGENE", "numeroCompteur": "GE 20 kVA", "index": 1520, "unite": "h"}],
          "cles": [{"typeCle": "Portail", "quantite": 2}, {"typeCle": "Porte principale", "quantite": 2},
                   {"typeCle": "Chambres", "quantite": 4}]}


def parcours_api():
    deja: set[str] = set()
    claire, patrick = Compte("claire.martin@kupanga.demo"), Compte("patrick.mbuyi@kupanga.demo")
    lucas, ines, hugo, sophie = (Compte(e) for e in ("lucas.bernard@kupanga.demo", "ines.moreau@kupanga.demo",
                                                      "hugo.lefevre@kupanga.demo", "sophie.garnier@kupanga.demo"))
    grace, jonathan = Compte("grace.mukendi@kupanga.demo"), Compte("jonathan.ilunga@kupanga.demo")

    base_fr = {"pays": "FR", "devise": "EUR", "ville": "Lyon", "ascenseur": True, "meuble": False, "colocation": False,
               "disponibleDe": AUJOURDHUI.isoformat(), "modeChauffage": "GAZ"}
    f1 = creer_bien(claire, {**base_fr, "titre": "T3 lumineux avec balcon, Croix-Rousse", "typeBien": "APPARTEMENT",
                             "adresse": "12 rue d'Austerlitz", "codePostal": "69004", "latitude": 45.7745, "longitude": 4.8310,
                             "description": "Au 4e étage avec ascenseur, beau T3 traversant de 68 m² avec balcon filant "
                                            "plein sud. Séjour lumineux, cuisine équipée, deux chambres sur cour, "
                                            "salle de douche refaite. Commerces et métro C à 3 minutes.",
                             "surfaceHabitable": 68, "nombrePieces": 3, "nombreChambres": 2, "etage": 4,
                             "anneeConstruction": 1930, "classeEnergie": "C", "classeGes": "D",
                             "loyerMensuel": 1150, "chargesMensuelles": 90, "depotGarantie": 1150,
                             "equipements": ["PARKING"]}, ["immeuble", "salon", "cuisine", "chambre", "salon"])
    f2 = creer_bien(claire, {**base_fr, "titre": "Studio meublé, Presqu'île", "typeBien": "STUDIO", "meuble": True,
                             "adresse": "8 rue Mercière", "codePostal": "69002", "latitude": 45.7620, "longitude": 4.8325,
                             "description": "Studio meublé de 27 m² entièrement rénové, au cœur de la Presqu'île. "
                                            "Kitchenette équipée, lit double, bureau. Idéal étudiant ou jeune actif.",
                             "surfaceHabitable": 27, "nombrePieces": 1, "nombreChambres": 0, "etage": 2,
                             "anneeConstruction": 1890, "classeEnergie": "D", "classeGes": "C", "modeChauffage": "ELECTRIQUE",
                             "loyerMensuel": 690, "chargesMensuelles": 50, "depotGarantie": 1380},
                    ["salon", "chambre", "cuisine"])
    f3 = creer_bien(claire, {**base_fr, "titre": "Maison avec jardin, Villeurbanne", "typeBien": "MAISON",
                             "ville": "Villeurbanne", "adresse": "41 rue Francis de Pressensé", "codePostal": "69100",
                             "latitude": 45.7675, "longitude": 4.8800, "ascenseur": False,
                             "description": "Maison de ville de 112 m² sur deux niveaux avec jardin de 200 m² et garage. "
                                            "Quatre chambres, double séjour, cuisine ouverte. Écoles et tram T3 à pied.",
                             "surfaceHabitable": 112, "nombrePieces": 5, "nombreChambres": 4,
                             "anneeConstruction": 1975, "classeEnergie": "D", "classeGes": "E",
                             "loyerMensuel": 1690, "chargesMensuelles": 40, "depotGarantie": 1690,
                             "equipements": ["PARKING"]}, ["maison", "salon", "cuisine", "chambre"])
    f4 = creer_bien(claire, {**base_fr, "titre": "T2 rénové, Brotteaux", "typeBien": "APPARTEMENT",
                             "adresse": "57 boulevard des Belges", "codePostal": "69006", "latitude": 45.7700, "longitude": 4.8530,
                             "description": "T2 de 46 m² rénové avec goût, parquet, moulures, cuisine séparée. "
                                            "Proche du parc de la Tête d'Or.",
                             "surfaceHabitable": 46, "nombrePieces": 2, "nombreChambres": 1, "etage": 3,
                             "anneeConstruction": 1910, "classeEnergie": "D", "classeGes": "D",
                             "loyerMensuel": 890, "chargesMensuelles": 70, "depotGarantie": 890},
                    ["immeuble", "salon", "chambre"])

    base_cd = {"pays": "CD", "devise": "USD", "ville": "Kinshasa", "meuble": False, "colocation": False,
               "disponibleDe": AUJOURDHUI.isoformat()}
    r1 = creer_bien(patrick, {**base_cd, "titre": "Villa 4 chambres avec piscine, Ma Campagne", "typeBien": "VILLA",
                              "adresse": "n° 14, avenue Colonel Mondjiba", "commune": "Ngaliema", "quartier": "Ma Campagne", "numeroParcelle": "2417",
                              "pointDeRepere": "Portail noir, en face de la station Total", "latitude": -4.3385, "longitude": 15.2662,
                              "description": "Belle villa de plain-pied sur parcelle clôturée de 900 m². Quatre chambres "
                                             "dont deux suites, grand salon climatisé, piscine, dépendance pour le "
                                             "personnel. Forage, groupe électrogène 20 kVA et gardiennage jour et nuit.",
                              "surfaceHabitable": 320, "nombrePieces": 7, "nombreChambres": 4,
                              "loyerMensuel": 3200, "chargesMensuelles": 0, "garantieMois": 3, "avanceMois": 6,
                              "heuresElectriciteParJour": 18,
                              "equipements": ["FORAGE", "GROUPE_ELECTROGENE", "CLOTURE", "GARDIENNAGE", "PARKING",
                                              "CLIMATISATION", "ELECTRICITE_SNEL"]},
                     ["villa", "villa", "salon", "chambre", "cuisine"])
    r2 = creer_bien(patrick, {**base_cd, "titre": "Appartement 3 pièces, Gombe", "typeBien": "APPARTEMENT",
                              "adresse": "n° 7, avenue de la Justice", "commune": "Gombe", "quartier": "Golf", "numeroParcelle": "88",
                              "pointDeRepere": "Immeuble Les Palmiers, 2e étage", "latitude": -4.3040, "longitude": 15.3025,
                              "description": "Appartement de 110 m² dans une résidence sécurisée : deux chambres, "
                                             "salon climatisé, cuisine équipée. Eau REGIDESO et citerne, panneaux solaires.",
                              "surfaceHabitable": 110, "nombrePieces": 3, "nombreChambres": 2,
                              "loyerMensuel": 1400, "chargesMensuelles": 0, "garantieMois": 3, "avanceMois": 3,
                              "heuresElectriciteParJour": 20,
                              "equipements": ["EAU_REGIDESO", "CITERNE", "PANNEAUX_SOLAIRES", "GARDIENNAGE", "CLIMATISATION"]},
                     ["immeuble", "salon", "chambre", "cuisine"])
    creer_bien(patrick, {**base_cd, "devise": "CDF", "titre": "Annexe 2 pièces, Limete Résidentiel", "typeBien": "ANNEXE",
                         "adresse": "n° 3, 7e rue", "commune": "Limete", "quartier": "Résidentiel",
                         "pointDeRepere": "Derrière l'église Saint-Raphaël",
                         "description": "Annexe indépendante de deux pièces dans une parcelle familiale calme, entrée "
                                        "séparée, douche intérieure. Compteur Cash Power individuel.",
                         "surfaceHabitable": 38, "nombrePieces": 2, "nombreChambres": 1,
                         "loyerMensuel": 700000, "chargesMensuelles": 0, "garantieMois": 2, "avanceMois": 3,
                         "heuresElectriciteParJour": 12, "equipements": ["EAU_REGIDESO", "CLOTURE", "ELECTRICITE_SNEL"]},
               ["maison", "salon", "chambre"])
    print("biens publiés par l'API :", f1, f2, f3, f4, r1, r2)

    # Conversations candidates (obligatoires avant une demande de location)
    conversation(f1, lucas.email, claire.email, [("c", "Bonjour Claire, le T3 de la Croix-Rousse est-il encore libre ?"),
                                                 ("p", "Bonjour Lucas, oui ! Visite possible jeudi 18 h ?"),
                                                 ("c", "Parfait, à jeudi."), ("p", "Ravie que ça vous plaise, je vous envoie la proposition.")], 240)
    conversation(f2, ines.email, claire.email, [("c", "Bonjour, le studio est-il disponible au 1er du mois ?"),
                                                ("p", "Oui, tout à fait. Il est meublé et prêt à vivre.")], 70)
    conversation(f3, hugo.email, claire.email, [("c", "Bonjour, nous sommes une famille de 4, la maison nous intéresse beaucoup."),
                                                ("p", "Bonjour Hugo, avec plaisir. Visite samedi 10 h ?"),
                                                ("c", "Merci, la visite nous a convaincus !")], 30)
    conversation(f4, sophie.email, claire.email, [("c", "Bonjour, je suis intéressée par le T2 des Brotteaux."),
                                                  ("p", "Bonjour Sophie, il est disponible tout de suite.")], 400)
    conversation(r1, grace.email, patrick.email, [("c", "Mbote Patrick, la villa de Ma Campagne est-elle libre ?"),
                                                  ("p", "Mbote Grâce ! Oui, visite possible demain 10 h."),
                                                  ("c", "Merci, je serai là avec mon mari.")], 200)
    conversation(r2, jonathan.email, patrick.email, [("c", "Bonjour, l'appartement de la Gombe a-t-il un groupe ?"),
                                                     ("p", "Bonjour, il a des panneaux solaires et une citerne.")], 20)

    # F1 : loué à Lucas, bail + EDL signés, quittance payée avec reçu, suivante en attente
    debut = AUJOURDHUI
    c1 = louer(claire, lucas, f1, {"dateDebut": debut.isoformat(), "dureeBailMois": 36, "loyerMensuel": 1150,
                                   "chargesMensuelles": 90, "depotGarantie": 1150}, EDL_FR, deja)
    m = AUJOURDHUI.month
    quittance(claire, f1, lucas.email, c1, m, AUJOURDHUI.year,
              {"modePaiement": "VIREMENT", "datePaiement": AUJOURDHUI.isoformat(), "referencePaiement": "VIR-LB-1150"},
              "AVIS DE VIREMENT\nDe : Lucas Bernard\nÀ : Claire Martin\nMontant : 1 240,00 EUR\nRéf. VIR-LB-1150")
    suivant = date(AUJOURDHUI.year + (m == 12), m % 12 + 1, 1)
    quittance(claire, f1, lucas.email, c1, suivant.month, suivant.year, None)

    # F4 : loué à Sophie, puis congé donné par la locataire
    c4 = louer(claire, sophie, f4, {"dateDebut": debut.isoformat(), "dureeBailMois": 12, "loyerMensuel": 890,
                                    "chargesMensuelles": 70, "depotGarantie": 890}, None, deja)
    quittance(claire, f4, sophie.email, c4, m, AUJOURDHUI.year,
              {"modePaiement": "CHEQUE", "datePaiement": AUJOURDHUI.isoformat(), "referencePaiement": "CHQ 0048812"})
    loc4 = int(sql(f"select id from locations where bien_id = {f4} and statut = 'ACTIVE'"))
    sophie.req("POST", f"/locations/{loc4}/conge", json={"dateFin": (AUJOURDHUI + timedelta(days=30)).isoformat()})
    # Démo : le congé arrive à échéance aujourd'hui (la fin du bail peut être enregistrée pendant les captures)
    sql(f"UPDATE locations SET date_fin_prevue = {q(AUJOURDHUI)}, date_conge = {q(datetime.now() - timedelta(days=30))} WHERE id = {loc4};")

    # F2 : réservé par Inès (demande acceptée, bail pas encore signé)
    d2 = claire.req("POST", f"/biens/{f2}/demandes-location/{uid(ines.email)}")
    ines.req("POST", f"/demandes-location/{d2['id']}/accepter")

    # F3 : demande en attente de réponse de Hugo
    claire.req("POST", f"/biens/{f3}/demandes-location/{uid(hugo.email)}")

    # R1 : villa louée à Grâce (garantie et avance en mois), EDL avec Cash Power, loyer payé en Mobile Money
    cr1 = louer(patrick, grace, r1, {"dateDebut": debut.isoformat(), "dureeBailMois": 24, "loyerMensuel": 3200,
                                     "chargesMensuelles": 0, "garantieMois": 3, "avanceMois": 6}, EDL_CD, deja)
    quittance(patrick, r1, grace.email, cr1, m, AUJOURDHUI.year,
              {"modePaiement": "MOBILE_MONEY", "datePaiement": AUJOURDHUI.isoformat(), "referencePaiement": "MPESA 7HX29KQ1"},
              "M-PESA  Transaction confirmée\nRéf. 7HX29KQ1\nMontant : 3 200,00 USD\nDe : Grâce Mukendi\nÀ : Patrick Mbuyi")
    quittance(patrick, r1, grace.email, cr1, suivant.month, suivant.year, None)

    # R2 : demande envoyée à Jonathan
    patrick.req("POST", f"/biens/{r2}/demandes-location/{uid(jonathan.email)}")

    # Notifications déjà lues pour les plus anciennes ? On les laisse non lues : la cloche est remplie.
    print("parcours API terminés")


def main():
    attendre_api()
    if sql("select count(*) from utilisateurs") != "0":
        sys.exit("La base de démo n'est pas vide : docker compose -p kupanga-demo -f demo/docker-compose-demo.yml down -v")
    stock = deposer_photos()
    remplir_volume(stock)
    parcours_api()
    print(sql("select 'biens', count(*) from biens union all select 'comptes', count(*) from utilisateurs "
              "union all select 'contrats', count(*) from contrats union all select 'quittances', count(*) from quittances "
              "union all select 'e-mails', count(*) from emails_sortants"))


if __name__ == "__main__":
    main()
