"""
PDF réels de la démo (bail, état des lieux, quittance ; France et RDC) rendus en PNG pour la vidéo.
Les PDF sont lus dans les buckets privés du stockage de démo (clés en base).

Lancement (depuis demo/captures, après captures.mjs) : python -I pdf.py [dossier de sortie]
"""
import subprocess
import sys
from pathlib import Path

import boto3
import fitz  # PyMuPDF

SORTIE = Path(sys.argv[1] if len(sys.argv) > 1 else "out/shots")
SORTIE.mkdir(parents=True, exist_ok=True)
s3 = boto3.client("s3", endpoint_url="http://localhost:9200", aws_access_key_id="kupanga-dev",
                  aws_secret_access_key="kupanga-dev-secret", region_name="us-east-1")


def sql(q: str) -> list[list[str]]:
    r = subprocess.run(["docker", "exec", "-i", "kupanga-demo-db", "psql", "-U", "kupanga", "-d", "kupanga_demo",
                        "-t", "-A", "-F", "|"], input=q.encode(), capture_output=True, check=True)
    return [l.split("|") for l in r.stdout.decode().splitlines() if l.strip()]


DOCUMENTS = [  # nom, bucket, requête (clé du PDF du document le plus récent)
    ("10-pdf-bail-fr", "contrat-de-bail", "select c.cle_pdf from contrats c join biens b on b.id = c.bien_id "
     "where b.pays = 'FR' and c.statut = 'SIGNE' and c.cle_pdf is not null order by c.id desc limit 1"),
    ("10-pdf-bail-rdc", "contrat-de-bail", "select c.cle_pdf from contrats c join biens b on b.id = c.bien_id "
     "where b.pays = 'CD' and c.statut = 'SIGNE' and c.cle_pdf is not null order by c.id desc limit 1"),
    ("10-pdf-edl-fr", "bucket-etats-des-lieux", "select e.cle_pdf from etats_des_lieux e join biens b on b.id = e.bien_id "
     "where b.pays = 'FR' and e.cle_pdf is not null order by e.id desc limit 1"),
    ("10-pdf-edl-rdc", "bucket-etats-des-lieux", "select e.cle_pdf from etats_des_lieux e join biens b on b.id = e.bien_id "
     "where b.pays = 'CD' and e.cle_pdf is not null order by e.id desc limit 1"),
    ("10-pdf-quittance-fr", "bucket-des-quittances", "select q.cle_pdf from quittances q join biens b on b.id = q.bien_id "
     "where b.pays = 'FR' and q.statut = 'PAYEE' and q.cle_pdf is not null order by q.id desc limit 1"),
    ("10-pdf-quittance-rdc", "bucket-des-quittances", "select q.cle_pdf from quittances q join biens b on b.id = q.bien_id "
     "where b.pays = 'CD' and q.statut = 'PAYEE' and q.cle_pdf is not null order by q.id desc limit 1"),
]

for nom, bucket, requete in DOCUMENTS:
    lignes = sql(requete)
    if not lignes:
        print("aucun document :", nom)
        continue
    cle = lignes[0][0]
    donnees = s3.get_object(Bucket=bucket, Key=cle)["Body"].read()
    (SORTIE / f"{nom}.pdf").write_bytes(donnees)
    doc = fitz.open(stream=donnees, filetype="pdf")
    for i, page in enumerate(doc):
        if i >= 4:
            break
        page.get_pixmap(dpi=220).save(SORTIE / f"{nom}-p{i + 1}.png")
    print(nom, doc.page_count, "page(s)")
