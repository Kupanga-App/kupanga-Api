import React from 'react';
import {AbsoluteFill, Audio, Sequence, Series, interpolate, staticFile, useVideoConfig} from 'remotion';
import {VIDEO_CONFIG} from './config';
import {Fondu} from './kit';
import {PLANS} from './plans';
import {SCENES, type Donnees} from './timing';

/**
 * La vidéo : les scènes à la suite. Dans une scène, le carton de chapitre (s'il y en a un) puis les plans,
 * qui se partagent le reste de la durée ; la voix off démarre après le carton.
 */
export const KupangaVideo: React.FC<Donnees> = (d) => {
  const {durationInFrames: total, fps} = useVideoConfig();
  const volumeMusique = (f: number) =>
    interpolate(f, [0, fps, total - 3 * fps, total], [0, VIDEO_CONFIG.musicVolume, VIDEO_CONFIG.musicVolume, 0], {
      extrapolateLeft: 'clamp', extrapolateRight: 'clamp',
    });
  return (
    <AbsoluteFill style={{background: '#0A161D'}}>
      <Series>
        {d.scenes.map((t) => {
          const scene = SCENES.find((s) => s.id === t.id)!;
          const carton = scene.plans[0]?.type === 'chapitre' ? scene.plans[0] : null;
          const plans = scene.plans.filter((p) => p.type !== 'chapitre');
          const reste = t.frames - t.chapterFrames;
          const parPlan = Math.floor(reste / Math.max(1, plans.length));
          return (
            <Series.Sequence key={t.id} durationInFrames={t.frames} name={t.id}>
              {carton && (
                <Sequence durationInFrames={t.chapterFrames} name="chapitre">
                  {React.createElement(PLANS.chapitre, {plan: carton, dur: t.chapterFrames, chapitre: '', d})}
                </Sequence>
              )}
              {plans.map((plan, i) => {
                const debut = t.chapterFrames + i * parPlan;
                const dur = i === plans.length - 1 ? t.frames - debut : parPlan;
                const Composant = PLANS[plan.type];
                const enchaine = plan.type === 'intro' || plan.type === 'outro';
                return (
                  <Sequence key={i} from={debut} durationInFrames={dur} name={`${plan.type} ${plan.src ?? plan.lien ?? ''}`}>
                    <Fondu dur={dur} entree={enchaine ? 1 : 10} sortie={enchaine ? 1 : 10}>
                      <Composant plan={plan} dur={dur} chapitre={scene.chapitre} d={d} />
                    </Fondu>
                  </Sequence>
                );
              })}
              {d.hasVoice && (
                <Sequence from={t.voiceFrom} name={`voix ${t.id}`}>
                  <Audio src={staticFile(`voice/${t.id}.mp3`)} />
                </Sequence>
              )}
            </Series.Sequence>
          );
        })}
      </Series>
      {VIDEO_CONFIG.music && <Audio src={staticFile(VIDEO_CONFIG.music)} volume={volumeMusique} loop />}
    </AbsoluteFill>
  );
};
