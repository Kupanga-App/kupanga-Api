import React from 'react';
import {AbsoluteFill, Easing, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {VIDEO_CONFIG} from './config';
import {C, sans, serif} from './theme';

export const ease = Easing.bezier(0.45, 0, 0.2, 1);
export const shot = (nom: string) => staticFile(`shots/${nom}.jpg`);

/** Fond nuit avec deux halos olive qui dérivent lentement. */
export const Fond: React.FC<{children?: React.ReactNode; clair?: boolean}> = ({children, clair}) => {
  const f = useCurrentFrame();
  const x = 30 + Math.sin(f / 180) * 8;
  const y = 25 + Math.cos(f / 220) * 6;
  return (
    <AbsoluteFill
      style={{
        background: clair
          ? `radial-gradient(1200px 800px at ${x}% ${y}%, #ffffff 0%, ${C.paper} 60%, #e6ead9 100%)`
          : `radial-gradient(1100px 700px at ${x}% ${y}%, rgba(143,164,100,0.22) 0%, rgba(143,164,100,0) 60%),
             radial-gradient(900px 700px at ${100 - x}% ${100 - y}%, rgba(143,164,100,0.12) 0%, rgba(143,164,100,0) 60%),
             linear-gradient(160deg, ${C.nightSoft} 0%, ${C.night} 45%, ${C.nightDeep} 100%)`,
      }}
    >
      {children}
    </AbsoluteFill>
  );
};

export const Logo: React.FC<{size: number; style?: React.CSSProperties}> = ({size, style}) => (
  <Img src={staticFile(VIDEO_CONFIG.logo)} style={{width: size, height: size, ...style}} />
);

/** Logo et nom en haut à gauche, chapitre en cours à droite. */
export const EnTete: React.FC<{chapitre: string}> = ({chapitre}) => (
  <div style={{position: 'absolute', top: 34, left: 56, right: 56, display: 'flex', alignItems: 'center', gap: 14}}>
    <Logo size={42} style={{borderRadius: 10}} />
    <span style={{fontFamily: serif, fontSize: 32, color: C.mint, letterSpacing: 0.3}}>Kupanga</span>
    <span style={{flex: 1}} />
    {chapitre && (
      <span style={{fontFamily: sans, fontWeight: 500, fontSize: 21, color: C.mint, padding: '9px 20px', borderRadius: 999,
        background: 'rgba(239,251,219,0.08)', border: '1px solid rgba(239,251,219,0.16)'}}>
        {chapitre}
      </span>
    )}
  </div>
);

/** Légende du plan (description de la capture), en bas. */
export const Legende: React.FC<{texte?: string; dur: number}> = ({texte, dur}) => {
  const f = useCurrentFrame();
  if (!texte || !VIDEO_CONFIG.legendes) return null;
  const o = interpolate(f, [8, 22, dur - 12, dur], [0, 1, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const y = interpolate(f, [8, 22], [14, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: ease});
  return (
    <div style={{position: 'absolute', left: 0, right: 0, bottom: 34, display: 'flex', justifyContent: 'center', opacity: o,
      transform: `translateY(${y}px)`}}>
      <div style={{fontFamily: sans, fontWeight: 500, fontSize: 25, color: C.night, background: C.mint, padding: '12px 26px',
        borderRadius: 14, boxShadow: '0 10px 40px rgba(0,0,0,0.35)', maxWidth: 1500, textAlign: 'center',
        display: 'flex', alignItems: 'center', gap: 14}}>
        <span style={{width: 10, height: 10, borderRadius: 5, background: C.olive, flex: 'none'}} />
        {texte}
      </div>
    </div>
  );
};

/** Fenêtre de navigateur. */
export const Navigateur: React.FC<{largeur: number; hauteur: number; url?: string; children: React.ReactNode;
  style?: React.CSSProperties; sombre?: boolean}> = ({largeur, hauteur, url, children, style, sombre}) => (
  <div style={{width: largeur, height: hauteur + 44, borderRadius: 18, overflow: 'hidden', background: sombre ? '#1c2a33' : '#fff',
    boxShadow: '0 40px 120px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.08)', ...style}}>
    <div style={{height: 44, display: 'flex', alignItems: 'center', gap: 9, padding: '0 18px', background: sombre ? '#22323c' : '#eef0ea'}}>
      {['#ff5f57', '#febc2e', '#28c840'].map((c) => <span key={c} style={{width: 13, height: 13, borderRadius: 7, background: c}} />)}
      <div style={{marginLeft: 18, flex: 1, maxWidth: 620, height: 28, borderRadius: 8, background: sombre ? '#152029' : '#fff',
        display: 'flex', alignItems: 'center', padding: '0 14px', fontFamily: sans, fontSize: 15, color: sombre ? '#9fb0b8' : '#6b7a73'}}>
        {url ?? 'kupanga.lespacelibellule.com'}
      </div>
    </div>
    <div style={{width: largeur, height: hauteur, overflow: 'hidden', position: 'relative'}}>{children}</div>
  </div>
);

/** Téléphone (écran 390 × 844 mis à l'échelle). */
export const Telephone: React.FC<{hauteur: number; children: React.ReactNode; style?: React.CSSProperties}> = ({hauteur, children, style}) => {
  const ecranH = hauteur - 28;
  const ecranW = (ecranH * 390) / 844;
  return (
    <div style={{width: ecranW + 28, height: hauteur, borderRadius: 62, background: '#0b0b0d', padding: 14,
      boxShadow: '0 40px 110px rgba(0,0,0,0.6), inset 0 0 0 2px #2a2a2e', position: 'relative', ...style}}>
      <div style={{width: ecranW, height: ecranH, borderRadius: 48, overflow: 'hidden', position: 'relative', background: '#fff'}}>
        {children}
      </div>
      <div style={{position: 'absolute', top: 26, left: '50%', width: 118, height: 32, marginLeft: -59, borderRadius: 20, background: '#000'}} />
    </div>
  );
};

/** Entrée d'un élément : glissé + fondu, en ressort. */
export function useEntree(delai = 0) {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const s = spring({frame: f - delai, fps, config: {damping: 200, mass: 0.9}});
  return {opacity: s, transform: `translateY(${(1 - s) * 40}px) scale(${0.97 + 0.03 * s})`};
}

/** Fondu d'entrée et de sortie d'un plan. */
export const Fondu: React.FC<{dur: number; children: React.ReactNode; entree?: number; sortie?: number}> = ({dur, children, entree = 10, sortie = 10}) => {
  const f = useCurrentFrame();
  const o = interpolate(f, [0, entree, dur - sortie, dur], [0, 1, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return <AbsoluteFill style={{opacity: o}}>{children}</AbsoluteFill>;
};
