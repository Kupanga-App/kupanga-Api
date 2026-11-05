import {staticFile} from 'remotion';
import scenario from '../scenario.json';

export const FPS = 30;

export type Plan = {
  type: 'intro' | 'promesse' | 'outro' | 'chapitre' | 'ecran' | 'mobile' | 'duo' | 'defile' | 'email' | 'pdf' | 'whatsapp';
  src?: string | string[];
  mobile?: string;
  ecran?: string;
  fond?: string;
  zoom?: [number, number, number, number];
  defile?: boolean;
  titre?: string;
  numero?: number;
  lien?: string;
};
export type Scene = {id: string; chapitre: string; texte: string; plans: Plan[]};
export type SceneTiming = {id: string; frames: number; voiceFrom: number; chapterFrames: number};

export const SCENES = scenario.scenes as Scene[];

export function defaultTimings(): SceneTiming[] {
  return SCENES.map((s) => {
    const carton = s.plans[0]?.type === 'chapitre' ? 72 : 0;
    return {id: s.id, frames: carton + Math.max(1, s.plans.length) * 108, voiceFrom: carton + 18, chapterFrames: carton};
  });
}

export type Donnees = {
  scenes: SceneTiming[];
  hasVoice: boolean;
  manifeste: Record<string, {description: string; appareil: string; url: string}>;
  tailles: Record<string, [number, number]>;
  whatsapp: Record<string, {texte: string; numero: string; url: string; description: string}>;
};

async function json<T>(chemin: string, defaut: T): Promise<T> {
  try {
    const r = await fetch(staticFile(chemin));
    return r.ok ? ((await r.json()) as T) : defaut;
  } catch {
    return defaut;
  }
}

/** Minutage (voix off), descriptions et tailles des captures. Sans voix : durées par défaut, vidéo muette. */
export async function charger(): Promise<Donnees> {
  const timing = await json<{scenes: SceneTiming[]} | null>('voice/timing.json', null);
  const parId = new Map((timing?.scenes ?? []).map((s) => [s.id, s]));
  return {
    scenes: defaultTimings().map((d) => parId.get(d.id) ?? d),
    hasVoice: timing !== null,
    manifeste: await json('shots/manifeste.json', {}),
    tailles: await json('shots/tailles.json', {}),
    whatsapp: await json('shots/whatsapp.json', {}),
  };
}
