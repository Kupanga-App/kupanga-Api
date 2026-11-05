// Outils communs des captures : contextes ordinateur (4K) et téléphone, connexion, captures, Mailpit.
import { chromium } from 'playwright';
import { execSync } from 'node:child_process';
import { mkdirSync, writeFileSync, readFileSync, existsSync } from 'node:fs';

export const FRONT = 'http://localhost:4300';
export const API = 'http://localhost:8189';
export const MAILPIT = 'http://localhost:8125';
export const MDP = 'Kupanga-Demo-2026';
export const SORTIE = process.env.SORTIE ?? 'out/shots';
const ECHELLE = Number(process.env.ECHELLE ?? 2); // 2 → captures ordinateur en 3840 × 2160

mkdirSync(SORTIE, { recursive: true });
const FICHIER_MANIFESTE = `${SORTIE}/manifeste.json`;
const manifeste = existsSync(FICHIER_MANIFESTE) ? JSON.parse(readFileSync(FICHIER_MANIFESTE, 'utf8')) : {};

export const navigateur = await chromium.launch({ args: ['--hide-scrollbars', '--font-render-hinting=none'] });

const CSS_PROPRE = `
  *, *::before, *::after { caret-color: transparent !important; }
  ::-webkit-scrollbar { display: none !important; }
`;

/** Contexte « ordinateur » (1920 × 1080 CSS) ou « téléphone » (390 × 844 CSS, iPhone). */
export async function contexte(type = 'ordinateur', { sombre = false } = {}) {
  const base = { locale: 'fr-FR', timezoneId: 'Europe/Paris', colorScheme: sombre ? 'dark' : 'light' };
  const ctx = await navigateur.newContext(type === 'telephone'
    ? { ...base, viewport: { width: 390, height: 844 }, deviceScaleFactor: 3, isMobile: true, hasTouch: true,
        userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1' }
    : { ...base, viewport: { width: 1920, height: 1080 }, deviceScaleFactor: ECHELLE });
  // Pas de bandeau d'installation de la PWA sur les captures (sauf demande explicite)
  await ctx.addInitScript(() => {
    try { localStorage.setItem('kp_installation_refus', String(Date.now())); } catch { /* */ }
  });
  await ctx.grantPermissions(['geolocation'], { origin: FRONT });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => console.warn('  [erreur page]', e.message));
  await page.addStyleTag({ content: CSS_PROPRE }).catch(() => {});
  page.on('load', () => page.addStyleTag({ content: CSS_PROPRE }).catch(() => {}));
  page.type_ = type;
  return page;
}

export function viderLimites() {
  execSync('docker exec kupanga-demo-redis redis-cli FLUSHALL');
}

export async function connexion(page, email) {
  viderLimites();
  await page.goto(`${FRONT}/auth/login`);
  await page.fill('#email', email);
  await page.fill('#password', MDP);
  await page.click('button[type=submit]');
  await page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 30000 });
  await calme(page);
}

export async function deconnexion(page) {
  await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
  await page.context().clearCookies();
}

/** Attend la fin des requêtes, des images et des tuiles de carte. */
export async function calme(page, ms = 700) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  await page.evaluate(async () => {
    const imgs = [...document.images].filter((i) => !i.complete);
    await Promise.race([Promise.all(imgs.map((i) => new Promise((r) => { i.onload = i.onerror = r; }))),
      new Promise((r) => setTimeout(r, 8000))]);
    await document.fonts?.ready;
  }).catch(() => {});
  await page.waitForTimeout(ms);
}

export async function aller(page, chemin) {
  await page.goto(chemin.startsWith('http') ? chemin : FRONT + chemin);
  await calme(page);
}

/**
 * Capture d'écran enregistrée sous <SORTIE>/<nom>.png et décrite dans le manifeste
 * (titre court, fonctionnalité montrée, appareil) : sert à la vidéo, au portfolio et à la landing page.
 */
export async function capture(page, nom, description, { pleinePage = false, element = null } = {}) {
  await calme(page, 400);
  const chemin = `${SORTIE}/${nom}.png`;
  if (element) await page.locator(element).first().screenshot({ path: chemin });
  else if (pleinePage) {
    // Fenêtre agrandie à la hauteur du contenu : la barre latérale et la barre du bas restent à leur place
    const taille = page.viewportSize();
    const hauteur = await page.evaluate(() => Math.max(document.documentElement.scrollHeight,
      ...[...document.querySelectorAll('main, .kp-page, [class*=contenu]')].map((e) => e.scrollHeight + e.getBoundingClientRect().top)));
    await page.setViewportSize({ width: taille.width, height: Math.min(Math.ceil(hauteur), 6000) });
    await calme(page, 600);
    await page.screenshot({ path: chemin });
    await page.setViewportSize(taille);
  } else await page.screenshot({ path: chemin });
  manifeste[nom] = { description, appareil: page.type_, pleinePage, url: page.url().replace(FRONT, '') };
  writeFileSync(FICHIER_MANIFESTE, JSON.stringify(manifeste, null, 1));
  console.log('  ✓', nom);
}

// ───── Mailpit ─────
export async function dernierEmail(destinataire, sujetContient = '', attente = 60000) {
  const fin = Date.now() + attente;
  while (Date.now() < fin) {
    const r = await (await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent('to:' + destinataire)}&limit=20`)).json();
    const m = (r.messages ?? []).find((x) => x.Subject.toLowerCase().includes(sujetContient.toLowerCase()));
    if (m) return m;
    await new Promise((r) => setTimeout(r, 1500));
  }
  throw new Error(`Aucun e-mail « ${sujetContient} » pour ${destinataire}`);
}

export async function lienDansEmail(id, motif) {
  const m = await (await fetch(`${MAILPIT}/api/v1/message/${id}`)).json();
  const lien = (m.HTML.match(/href="([^"]+)"/g) ?? []).map((h) => h.slice(6, -1).replaceAll('&amp;', '&'))
    .find((h) => h.includes(motif));
  if (!lien) throw new Error(`Lien « ${motif} » introuvable dans l'e-mail ${id}`);
  return lien;
}

/** Ouvre un e-mail dans l'interface de Mailpit (vue HTML) et le capture. */
export async function captureEmail(page, id, nom, description) {
  await aller(page, `${MAILPIT}/view/${id}`);
  await page.waitForTimeout(800);
  await capture(page, nom, description);
}

export function sql(requete) {
  return execSync(`docker exec -i kupanga-demo-db psql -U kupanga -d kupanga_demo -t -A`, { input: requete }).toString().trim();
}

export async function fin() {
  await navigateur.close();
}
