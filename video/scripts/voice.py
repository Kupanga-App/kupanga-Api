"""
Voix off de la vidéo Kupanga (Edge TTS : gratuit, sans compte), à partir de scenario.json.

Pour chaque scène : public/voice/<scène>.mp3, puis
- public/voice/timing.json : durée de chaque scène, calée sur sa voix (lue par Remotion) ;
- public/voice/kupanga-demo.srt : sous-titres (YouTube) ;
- public/voice/chapitres-youtube.txt : chapitres à coller dans la description YouTube.

Installation : pip install "edge-tts>=7" mutagen        Lancement (depuis video/) : npm run voice
"""
import asyncio
import json
import math
import re
from pathlib import Path

import edge_tts
from mutagen.mp3 import MP3

VOICE = "fr-FR-HenriNeural"
RATE = "+0%"
PITCH = "-2Hz"
FPS = 30
PAD_IN = 0.6       # silence avant la voix (s)
PAD_OUT = 0.9      # respiration après la voix (s)
PAR_PLAN = 3.6     # durée minimale d'un plan (s)
CHAPITRE = 2.4     # durée du carton de chapitre (s), avant la voix
INTRO, OUTRO = 9.0, 9.0

# Prononciation de la marque ; les sous-titres gardent « Kupanga ».
SAY_BRAND = "Koupanga"
PRONONCIATION = {"Kupanga": SAY_BRAND, "REGIDESO": "Régidéso", "RDC": "R D C", "Mailpit": "Mail-pitt"}

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "public" / "voice"
SCENARIO = json.loads((ROOT / "scenario.json").read_text(encoding="utf-8"))["scenes"]


def parle(texte: str) -> str:
    for a, b in PRONONCIATION.items():
        texte = texte.replace(a, b)
    return texte


def ecrit(texte: str) -> str:
    for a, b in PRONONCIATION.items():
        texte = texte.replace(b, a)
    return texte


async def synth(scene_id: str, text: str) -> list[tuple[float, float, str]]:
    communicate = edge_tts.Communicate(parle(text), VOICE, rate=RATE, pitch=PITCH, boundary="SentenceBoundary")
    cues: list[tuple[float, float, str]] = []
    with open(OUT / f"{scene_id}.mp3", "wb") as audio:
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio.write(chunk["data"])
            elif chunk["type"] in ("SentenceBoundary", "WordBoundary"):
                start = chunk["offset"] / 1e7
                cues.append((start, start + chunk["duration"] / 1e7, ecrit(chunk["text"])))
    return decouper(cues)


def decouper(cues):
    """Phrases longues découpées en sous-titres de ~80 caractères, au prorata du temps."""
    sortie = []
    for start, end, text in cues:
        if len(text) <= 84:
            sortie.append((start, end, text))
            continue
        morceaux, courant = [], ""
        for mot in text.split():
            if len(courant) + len(mot) > 80 and courant:
                morceaux.append(courant)
                courant = mot
            else:
                courant = (courant + " " + mot).strip()
        morceaux.append(courant)
        total = sum(len(m) for m in morceaux)
        t = start
        for m in morceaux:
            d = (end - start) * len(m) / total
            sortie.append((t, t + d, m))
            t += d
    return sortie


def wrap(text: str, width: int = 44) -> str:
    if len(text) <= width:
        return text
    words = text.split()
    best, best_gap = len(words) // 2, 10**9
    for i in range(1, len(words)):
        gap = abs(len(" ".join(words[:i])) - len(" ".join(words[i:])))
        if gap < best_gap:
            best, best_gap = i, gap
    return " ".join(words[:best]) + "\n" + " ".join(words[best:])


def srt_time(seconds: float) -> str:
    ms = int(round(seconds * 1000))
    h, ms = divmod(ms, 3_600_000)
    m, ms = divmod(ms, 60_000)
    s, ms = divmod(ms, 1000)
    return f"{h:02}:{m:02}:{s:02},{ms:03}"


def hms(seconds: float) -> str:
    s = int(seconds)
    return f"{s // 60}:{s % 60:02}" if s < 3600 else f"{s // 3600}:{s % 3600 // 60:02}:{s % 60:02}"


async def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    timing, subtitles, chapitres = [], [], [("0:00", "Introduction")]
    offset = 0.0
    for scene in SCENARIO:
        sid, plans = scene["id"], scene["plans"]
        print(f"Voix {sid}…", flush=True)
        cues = await synth(sid, scene["texte"])
        length = MP3(OUT / f"{sid}.mp3").info.length
        carton = CHAPITRE if plans and plans[0]["type"] == "chapitre" else 0.0
        nb = len([p for p in plans if p["type"] != "chapitre"])
        minimum = INTRO if sid == SCENARIO[0]["id"] else OUTRO if sid == SCENARIO[-1]["id"] else nb * PAR_PLAN
        voix_debut = carton + PAD_IN
        duree = max(carton + minimum, voix_debut + length + PAD_OUT)
        frames = math.ceil(duree * FPS)
        timing.append({"id": sid, "frames": frames, "voiceFrom": round(voix_debut * FPS),
                       "chapterFrames": round(carton * FPS), "voiceSeconds": round(length, 2)})
        if carton:
            chapitres.append((hms(offset), plans[0]["titre"]))
        for start, end, line in cues:
            subtitles.append((offset + voix_debut + start, offset + voix_debut + end, line))
        offset += frames / FPS
    (OUT / "timing.json").write_text(json.dumps({"fps": FPS, "scenes": timing}, indent=2), encoding="utf-8")
    srt = [f"{i}\n{srt_time(a)} --> {srt_time(b)}\n{wrap(t)}\n" for i, (a, b, t) in enumerate(subtitles, 1)]
    (OUT / "kupanga-demo.srt").write_text("\n".join(srt), encoding="utf-8")
    (OUT / "chapitres-youtube.txt").write_text("\n".join(f"{t} {n}" for t, n in chapitres), encoding="utf-8")
    print(f"Terminé : {len(SCENARIO)} scènes, {offset / 60:.1f} min.")


if __name__ == "__main__":
    asyncio.run(main())
