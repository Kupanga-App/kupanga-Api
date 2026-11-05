import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {VIDEO_CONFIG} from './config';
import {EnTete, Fond, Legende, Logo, Navigateur, Telephone, ease, shot, useEntree} from './kit';
import {C, sans, serif} from './theme';
import type {Donnees, Plan} from './timing';

export type PlanProps = {plan: Plan; dur: number; chapitre: string; d: Donnees};

const LARGEUR = 1480; // fenêtre de navigateur
const HAUTEUR = (LARGEUR * 9) / 16;
const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

/** Progression 0 → 1 d'un mouvement de caméra, entre deux fractions du plan. */
function useMouvement(dur: number, debut = 0.12, fin = 0.62) {
  const f = useCurrentFrame();
  return interpolate(f, [dur * debut, dur * fin], [0, 1], {...clamp, easing: ease});
}

/** Capture 16:9 avec zoom progressif sur une zone [x, y, l, h] (fractions), ou léger travelling. */
const Zoom: React.FC<{src: string; largeur: number; hauteur: number; zoom?: Plan['zoom']; dur: number}> = ({src, largeur, hauteur, zoom, dur}) => {
  const t = useMouvement(dur);
  const f = useCurrentFrame();
  let s = 1 + 0.035 * (f / dur), tx = 0, ty = 0;
  if (zoom) {
    const [x, y, l, h] = zoom;
    const cible = Math.min(1 / l, 1 / h, 2.4);
    s = 1 + (cible - 1) * t;
    // Centre de la zone ramené au centre du cadre, sans sortir de l'image
    const cx = (x + l / 2) * largeur, cy = (y + h / 2) * hauteur;
    const maxX = (largeur * s - largeur) / 2, maxY = (hauteur * s - hauteur) / 2;
    tx = Math.max(-maxX, Math.min(maxX, (largeur / 2 - cx) * s)) * (s > 1 ? 1 : 0);
    ty = Math.max(-maxY, Math.min(maxY, (hauteur / 2 - cy) * s)) * (s > 1 ? 1 : 0);
  }
  return (
    <Img src={shot(src)} style={{width: largeur, height: hauteur, objectFit: 'cover', objectPosition: 'top',
      transform: `translate(${tx}px, ${ty}px) scale(${s})`, transformOrigin: 'center'}} />
  );
};

const Centre: React.FC<{children: React.ReactNode; haut?: number}> = ({children, haut = 104}) => (
  <AbsoluteFill style={{alignItems: 'center', paddingTop: haut}}>{children}</AbsoluteFill>
);

const description = (d: Donnees, nom?: string) => (nom ? d.manifeste[nom]?.description : undefined);
const url = (d: Donnees, nom: string) => {
  const u = d.manifeste[nom]?.url ?? '';
  if (u.startsWith('http://localhost:8125')) return 'Mailpit · boîte de test';
  if (u.startsWith('http://localhost:8189/backoffice')) return 'kupanga · back-office' + u.replace('http://localhost:8189/backoffice', '');
  return 'kupanga.lespacelibellule.com' + u.split('?')[0];
};

export const PlanEcran: React.FC<PlanProps> = ({plan, dur, chapitre, d}) => {
  const src = plan.src as string;
  const e = useEntree(0);
  return (
    <Fond>
      <EnTete chapitre={chapitre} />
      <Centre>
        <div style={e}>
          <Navigateur largeur={LARGEUR} hauteur={HAUTEUR} url={url(d, src)}>
            <Zoom src={src} largeur={LARGEUR} hauteur={HAUTEUR} zoom={plan.zoom} dur={dur} />
          </Navigateur>
        </div>
      </Centre>
      <Legende texte={description(d, src)} dur={dur} />
    </Fond>
  );
};

/** Page longue qui défile de haut en bas dans la fenêtre. */
export const PlanDefile: React.FC<PlanProps> = ({plan, dur, chapitre, d}) => {
  const src = plan.src as string;
  const [w, h] = d.tailles[src] ?? [3840, 2160];
  const hauteurImage = (LARGEUR * h) / w;
  const t = useMouvement(dur, 0.1, 0.92);
  const e = useEntree(0);
  return (
    <Fond>
      <EnTete chapitre={chapitre} />
      <Centre>
        <div style={e}>
          <Navigateur largeur={LARGEUR} hauteur={HAUTEUR} url={url(d, src)}>
            <Img src={shot(src)} style={{width: LARGEUR, height: hauteurImage,
              transform: `translateY(${-Math.max(0, hauteurImage - HAUTEUR) * t}px)`}} />
          </Navigateur>
        </div>
      </Centre>
      <Legende texte={description(d, src)} dur={dur} />
    </Fond>
  );
};

/** Téléphone au centre droit, légende en grand à gauche, capture d'ordinateur floutée derrière. */
export const PlanMobile: React.FC<PlanProps> = ({plan, dur, chapitre, d}) => {
  const src = plan.src as string;
  const e = useEntree(0);
  const t = useEntree(6);
  const f = useCurrentFrame();
  const [w, h] = d.tailles[src] ?? [1170, 2532];
  const hauteurTel = 900;
  const ecranW = ((hauteurTel - 28) * 390) / 844;
  const hauteurImage = (ecranW * h) / w;
  const defile = Math.max(0, hauteurImage - (hauteurTel - 28)) * interpolate(f, [dur * 0.25, dur * 0.85], [0, 1], {...clamp, easing: ease});
  return (
    <Fond>
      {plan.fond && (
        <Img src={shot(plan.fond)} style={{position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover',
          filter: 'blur(28px) brightness(0.35) saturate(0.9)', transform: 'scale(1.1)'}} />
      )}
      <EnTete chapitre={chapitre} />
      <AbsoluteFill style={{flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 120, paddingTop: 50}}>
        <div style={{width: 640, ...t}}>
          <div style={{fontFamily: serif, fontSize: 64, lineHeight: 1.08, color: C.mint}}>{description(d, src)}</div>
          <div style={{marginTop: 26, width: 90, height: 6, borderRadius: 3, background: C.olive}} />
        </div>
        <div style={e}>
          <Telephone hauteur={hauteurTel}>
            <Img src={shot(src)} style={{width: '100%', transform: `translateY(${-defile}px)`}} />
          </Telephone>
        </div>
      </AbsoluteFill>
    </Fond>
  );
};

/** Ordinateur et téléphone côte à côte (le même écran, deux appareils). */
export const PlanDuo: React.FC<PlanProps> = ({plan, dur, chapitre, d}) => {
  const src = plan.src as string;
  const [w, h] = d.tailles[src] ?? [3840, 2160];
  const L = 1300, H = (L * 9) / 16;
  const hauteurImage = (L * h) / w;
  const t = useMouvement(dur, 0.15, 0.9);
  const e1 = useEntree(0);
  const e2 = useEntree(10);
  return (
    <Fond>
      <EnTete chapitre={chapitre} />
      <AbsoluteFill style={{paddingTop: 120, paddingLeft: 110}}>
        <div style={e1}>
          <Navigateur largeur={L} hauteur={H} url={url(d, src)}>
            <Img src={shot(src)} style={{width: L, height: hauteurImage,
              transform: `translateY(${plan.defile ? -Math.max(0, hauteurImage - H) * t : 0}px)`}} />
          </Navigateur>
        </div>
      </AbsoluteFill>
      <div style={{position: 'absolute', right: 120, top: 140, ...e2}}>
        <Telephone hauteur={800}>
          <Img src={shot(plan.mobile!)} style={{width: '100%'}} />
        </Telephone>
      </div>
      <Legende texte={description(d, src)} dur={dur} />
    </Fond>
  );
};

/** E-mail reçu (vue Mailpit), zoom sur le message. */
export const PlanEmail: React.FC<PlanProps> = (p) => (
  <PlanEcran {...p} plan={{...p.plan, zoom: p.plan.zoom ?? [0.455, 0.255, 0.355, 0.62]}} />
);

/** Pages d'un PDF réel, déployées en éventail. */
export const PlanPdf: React.FC<PlanProps> = ({plan, dur, chapitre}) => {
  const pages = (plan.src as string[]).slice(0, 3);
  const f = useCurrentFrame();
  const zoom = interpolate(f, [dur * 0.3, dur], [1, 1.08], clamp);
  const n = pages.length;
  const titre = pages[0].includes('bail') ? 'Bail de location' : pages[0].includes('edl') ? 'État des lieux' : 'Quittance de loyer';
  const pays = pages[0].includes('rdc') ? 'RDC · modèle cd-v1' : 'France · modèle fr-v1';
  return (
    <Fond>
      <EnTete chapitre={chapitre} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', paddingTop: 40, transform: `scale(${zoom})`}}>
        {pages.map((p, i) => {
          const s = interpolate(f, [8 + i * 12, 30 + i * 12], [0, 1], {...clamp, easing: ease});
          const angle = (i - (n - 1) / 2) * 5;
          const x = (i - (n - 1) / 2) * 400;
          return (
            <Img key={p} src={shot(p)} style={{position: 'absolute', height: 860, borderRadius: 4, background: '#fff',
              boxShadow: '0 30px 80px rgba(0,0,0,0.5)', opacity: s,
              transform: `translate(${x * s}px, ${(1 - s) * 80}px) rotate(${angle * s}deg)`, zIndex: n - i}} />
          );
        })}
      </AbsoluteFill>
      <Legende texte={`PDF généré par Kupanga · ${titre} · ${pays}`} dur={dur} />
    </Fond>
  );
};

/** Message WhatsApp prérempli par l'API (lien wa.me), affiché dans un téléphone. */
export const PlanWhatsapp: React.FC<PlanProps> = ({plan, dur, chapitre, d}) => {
  const lien = d.whatsapp[plan.lien ?? ''];
  const f = useCurrentFrame();
  const texte = lien?.texte ?? '';
  const visibles = Math.round(interpolate(f, [12, dur * 0.65], [0, texte.length], clamp));
  const e = useEntree(0);
  const t = useEntree(6);
  const numero = lien?.numero ? `+${lien.numero}` : 'Contact à choisir';
  return (
    <Fond>
      <EnTete chapitre={chapitre} />
      <AbsoluteFill style={{flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 120, paddingTop: 50}}>
        <div style={{width: 660, ...t}}>
          <div style={{fontFamily: serif, fontSize: 62, lineHeight: 1.08, color: C.mint}}>{lien?.description ?? 'Message prérempli'}</div>
          <div style={{marginTop: 30, fontFamily: 'monospace', fontSize: 22, color: C.oliveLight, wordBreak: 'break-all'}}>
            {(lien?.url ?? '').slice(0, 64)}…
          </div>
        </div>
        <div style={e}>
          <Telephone hauteur={900}>
            <AbsoluteFill style={{background: '#efe7dd'}}>
              <div style={{height: 120, background: '#1f6f5c', display: 'flex', alignItems: 'flex-end', padding: '0 22px 16px', gap: 14}}>
                <div style={{width: 46, height: 46, borderRadius: 23, background: '#c9d8cf'}} />
                <div style={{fontFamily: sans, color: '#fff'}}>
                  <div style={{fontSize: 21, fontWeight: 700}}>{numero}</div>
                  <div style={{fontSize: 15, opacity: 0.8}}>Message prêt à envoyer</div>
                </div>
              </div>
              <div style={{padding: '26px 16px', display: 'flex', justifyContent: 'flex-end'}}>
                <div style={{maxWidth: 330, background: '#d9fdd3', borderRadius: '16px 4px 16px 16px', padding: '12px 14px',
                  fontFamily: sans, fontSize: 17.5, lineHeight: 1.38, color: '#111b21', whiteSpace: 'pre-wrap',
                  boxShadow: '0 1px 1px rgba(0,0,0,0.12)'}}>
                  {texte.slice(0, visibles)}
                  <span style={{opacity: f % 30 < 15 ? 1 : 0}}>▍</span>
                </div>
              </div>
            </AbsoluteFill>
          </Telephone>
        </div>
      </AbsoluteFill>
    </Fond>
  );
};

/** Carton de chapitre. */
export const PlanChapitre: React.FC<PlanProps> = ({plan, dur}) => {
  const f = useCurrentFrame();
  const o = interpolate(f, [0, 10, dur - 10, dur], [0, 1, 1, 0], clamp);
  const ligne = interpolate(f, [6, 34], [0, 220], {...clamp, easing: ease});
  const e = useEntree(4);
  return (
    <Fond>
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: o}}>
        <div style={{...e, textAlign: 'center'}}>
          <div style={{fontFamily: sans, fontWeight: 700, fontSize: 30, letterSpacing: 8, color: C.olive}}>
            CHAPITRE {String(plan.numero ?? '').padStart(2, '0')}
          </div>
          <div style={{fontFamily: serif, fontSize: 124, color: C.mint, marginTop: 18}}>{plan.titre}</div>
          <div style={{height: 6, width: ligne, background: C.olive, borderRadius: 3, margin: '30px auto 0'}} />
        </div>
      </AbsoluteFill>
    </Fond>
  );
};

const MOTS = ['Annonce', 'Visite', 'Messages', 'Bail', 'Signature', 'État des lieux', 'Quittance', 'Relances', 'Fin de bail'];

/** Intro : les étapes d'une location éparpillées, puis réunies dans Kupanga. */
export const PlanIntro: React.FC<PlanProps> = ({dur}) => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const regroupe = interpolate(f, [dur - 3.2 * fps, dur - 2.2 * fps], [0, 1], {...clamp, easing: ease});
  const logo = interpolate(f, [dur - 2.4 * fps, dur - 1.6 * fps], [0, 1], {...clamp, easing: ease});
  return (
    <Fond>
      {MOTS.map((m, i) => {
        const a = (i / MOTS.length) * Math.PI * 2 + 0.4;
        const r = 330 + (i % 3) * 70;
        const apparition = interpolate(f, [10 + i * 14, 28 + i * 14], [0, 1], {...clamp, easing: ease});
        const flotte = Math.sin((f + i * 20) / 40) * 10;
        const x = Math.cos(a) * r * 1.5 * (1 - regroupe), y = Math.sin(a) * r * (1 - regroupe) + flotte;
        return (
          <AbsoluteFill key={m} style={{alignItems: 'center', justifyContent: 'center'}}>
            <div style={{transform: `translate(${x}px, ${y}px) scale(${1 - 0.6 * regroupe})`, opacity: apparition * (1 - regroupe),
              fontFamily: sans, fontWeight: 500, fontSize: 34, color: C.mint, padding: '16px 28px', borderRadius: 16,
              background: 'rgba(239,251,219,0.07)', border: '1px solid rgba(239,251,219,0.18)'}}>
              {m}
            </div>
          </AbsoluteFill>
        );
      })}
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: logo}}>
        <Logo size={150} style={{borderRadius: 34, transform: `scale(${0.8 + 0.2 * logo})`}} />
        <div style={{fontFamily: serif, fontSize: 120, color: C.mint, marginTop: 24}}>Kupanga</div>
      </AbsoluteFill>
    </Fond>
  );
};

/** Promesse : marque, France et RDC, ordinateur et téléphone. */
export const PlanPromesse: React.FC<PlanProps> = ({plan, dur}) => {
  const titre = useEntree(0);
  const ecran = useEntree(18);
  const tel = useEntree(30);
  const f = useCurrentFrame();
  const s = interpolate(f, [0, dur], [1, 1.04]);
  return (
    <Fond>
      <div style={{position: 'absolute', top: 70, left: 110, width: 640, ...titre}}>
        <Logo size={84} style={{borderRadius: 20}} />
        <div style={{fontFamily: serif, fontSize: 92, color: C.mint, lineHeight: 1.02, marginTop: 26}}>La gestion locative, de bout en bout.</div>
        <div style={{fontFamily: sans, fontSize: 30, color: C.muted, marginTop: 24, lineHeight: 1.4}}>
          De l’annonce à la fin du bail. Une seule application pour la France et la RDC.
        </div>
        <div style={{display: 'flex', gap: 14, marginTop: 34}}>
          {['France · €', 'RDC · $ · CDF', 'Ordinateur et téléphone'].map((x) => (
            <span key={x} style={{fontFamily: sans, fontWeight: 500, fontSize: 22, color: C.night, background: C.olive,
              padding: '10px 18px', borderRadius: 999}}>{x}</span>
          ))}
        </div>
      </div>
      <div style={{position: 'absolute', right: -160, top: 150, ...ecran}}>
        <div style={{transform: `scale(${s})`}}>
          <Navigateur largeur={1140} hauteur={(1140 * 9) / 16}>
            <Img src={shot(plan.ecran!)} style={{width: 1140}} />
          </Navigateur>
        </div>
      </div>
      <div style={{position: 'absolute', right: 820, top: 300, ...tel}}>
        <Telephone hauteur={700}><Img src={shot(plan.mobile!)} style={{width: '100%'}} /></Telephone>
      </div>
    </Fond>
  );
};

/** Fin : logo, promesse, adresse du site. */
export const PlanOutro: React.FC<PlanProps> = ({dur}) => {
  const a = useEntree(0);
  const b = useEntree(14);
  const c = useEntree(28);
  const f = useCurrentFrame();
  const o = interpolate(f, [dur - 25, dur], [1, 0], clamp);
  return (
    <Fond>
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: o, textAlign: 'center'}}>
        <div style={a}><Logo size={170} style={{borderRadius: 38}} /></div>
        <div style={{...b, fontFamily: serif, fontSize: 130, color: C.mint, marginTop: 20}}>Kupanga</div>
        <div style={{...c, fontFamily: sans, fontSize: 36, color: C.muted, marginTop: 8}}>
          De l’annonce à la fin du bail · France et République démocratique du Congo
        </div>
        {VIDEO_CONFIG.siteUrl && (
          <div style={{...c, marginTop: 46, fontFamily: sans, fontWeight: 700, fontSize: 32, color: C.night, background: C.olive,
            padding: '16px 34px', borderRadius: 999}}>{VIDEO_CONFIG.siteUrl}</div>
        )}
      </AbsoluteFill>
    </Fond>
  );
};

export const PLANS: Record<Plan['type'], React.FC<PlanProps>> = {
  intro: PlanIntro, promesse: PlanPromesse, outro: PlanOutro, chapitre: PlanChapitre, ecran: PlanEcran, mobile: PlanMobile,
  duo: PlanDuo, defile: PlanDefile, email: PlanEmail, pdf: PlanPdf, whatsapp: PlanWhatsapp,
};

void staticFile;
