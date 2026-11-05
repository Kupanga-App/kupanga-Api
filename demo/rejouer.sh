#!/usr/bin/env bash
# Remet la démo à zéro, la remplit, puis rejoue toutes les captures (et les PDF).
# Usage (depuis la racine du dépôt) : ECHELLE=2 SORTIE=out/shots bash demo/rejouer.sh
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -p kupanga-demo -f demo/docker-compose-demo.yml down -v
docker compose -p kupanga-demo -f demo/docker-compose-demo.yml up -d
PYTHONIOENCODING=utf-8 python -I demo/seed/remplir.py
cd demo/captures
rm -rf "${SORTIE:-out/shots}"
MSYS_NO_PATHCONV=1 node captures.mjs
PYTHONIOENCODING=utf-8 python -I pdf.py "${SORTIE:-out/shots}"
