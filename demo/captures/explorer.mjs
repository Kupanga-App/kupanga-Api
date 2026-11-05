// Tour rapide (basse définition) pour repérer les écrans : node explorer.mjs <email> <chemin1> <chemin2> ...
import { chromium } from 'playwright';
import { execSync } from 'node:child_process';

const FRONT = 'http://localhost:4300';
const [email, ...chemins] = process.argv.slice(2);
const OUT = process.env.OUT ?? 'out/explorer';
execSync('docker exec kupanga-demo-redis redis-cli FLUSHALL');

const navigateur = await chromium.launch();
const mobile = process.env.MOBILE === '1';
const ctx = await navigateur.newContext(mobile
  ? { viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, isMobile: true, hasTouch: true, locale: 'fr-FR' }
  : { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1, locale: 'fr-FR' });
const page = await ctx.newPage();
if (email && email !== '-') {
  await page.goto(`${FRONT}/auth/login`);
  await page.fill('#email', email);
  await page.fill('#password', 'Kupanga-Demo-2026');
  await page.click('button[type=submit]');
  await page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 });
}
for (const [i, c] of chemins.entries()) {
  await page.goto(FRONT + c);
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(1200);
  const nom = `${OUT}/${String(i).padStart(2, '0')}-${c.replace(/[^a-z0-9]+/gi, '_')}.png`;
  await page.screenshot({ path: nom, fullPage: process.env.FULL === '1' });
  console.log(nom);
}
await navigateur.close();
