# Vidéo de démo Kupanga (Remotion)

Vraies captures de l'application (environnement de démo, `../demo`), animées en React avec Remotion :
cadres d'ordinateur et de téléphone, zooms, pages qui défilent, PDF, e-mails, message WhatsApp, cartons de chapitre,
voix off (Edge TTS, gratuite) et musique de fond **en boucle** quelle que soit la durée.

- Scénario et voix off : `scenario.json` (une phrase par scène, les plans qui l'illustrent).
- Durée : ~10 min, 9 chapitres. Rendu 4K (3840 × 2160) : la composition est en 1080p, rendue avec `--scale=2`.

## Fabriquer la vidéo

```bash
npm install
pip install "edge-tts>=7" mutagen pillow

# 1. Captures 4K (voir ../demo/README.md), puis copie dans public/shots
npm run shots

# 2. Voix off + calage des scènes + sous-titres (.srt) + chapitres YouTube
npm run voice          # → public/voice/kupanga-demo.srt et chapitres-youtube.txt

# 3. Prévisualiser, puis rendu final 4K → out/kupanga-demo-4k.mp4
npm run studio
npm run render
```

- `npm run render:draft` : rendu rapide en demi-résolution pour vérifier le rythme.
- YouTube : téléverser `out/kupanga-demo-4k.mp4`, les sous-titres `public/voice/kupanga-demo.srt`,
  et coller `public/voice/chapitres-youtube.txt` dans la description.

## Réglages

- `src/config.ts` : adresse du site affichée à la fin, musique (`public/music.mp3`, celle de KitchUp), volume, logo, légendes.
- `scripts/voice.py` : voix (`VOICE`), débit (`RATE`), prononciation (`PRONONCIATION`), durée minimale d'un plan.
- Logo : `public/brand/kupanga-mark-a.svg` (pistes B et C à côté).
- Photos des annonces : CC0 (domaine public), StockSnap via Openverse (`../demo/seed/photos/credits.json`).
