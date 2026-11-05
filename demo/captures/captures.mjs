// Captures de l'app Kupanga pour la vidéo, la landing page et le portfolio.
// Prérequis : pile de démo remplie (demo/seed/remplir.py) et front servi en configuration demo (port 4300).
// Lancement : node captures.mjs [segment ...]   (sans argument : tous les segments, dans l'ordre)
import {
  FRONT, aller, calme, capture, captureEmail, connexion, contexte, dernierEmail, fin, lienDansEmail, sql,
} from './outils.mjs';

const C = {
  claire: 'claire.martin@kupanga.demo', patrick: 'patrick.mbuyi@kupanga.demo', lucas: 'lucas.bernard@kupanga.demo',
  ines: 'ines.moreau@kupanga.demo', hugo: 'hugo.lefevre@kupanga.demo', sophie: 'sophie.garnier@kupanga.demo',
  grace: 'grace.mukendi@kupanga.demo', jonathan: 'jonathan.ilunga@kupanga.demo', nadia: 'nadia.benali@kupanga.demo',
  merveille: 'merveille.kabongo@kupanga.demo',
};
const idBien = (titre) => Number(sql(`select max(id) from biens where titre = '${titre.replaceAll("'", "''")}'`));

// ───────────────────────────── 1. Visiteur : chercher un logement ─────────────────────────────
async function visiteur() {
  const p = await contexte('ordinateur');
  await aller(p, '/explorer');
  await capture(p, '01-explorer', 'Explorer : toutes les annonces, France et RDC, sans compte');

  await p.selectOption('#tri', { index: 1 });
  await calme(p);
  await capture(p, '01-explorer-tri', 'Tri des annonces (loyer, surface, date)');

  await p.getByRole('button', { name: 'Filtres' }).click();
  await p.selectOption('#f-pays', 'CD');
  await p.getByRole('button', { name: 'Villa', exact: true }).click().catch(() => {});
  await p.waitForTimeout(500);
  await capture(p, '01-filtres', 'Filtres : pays, ville, type, loyer, surface, meublé, écoles et pharmacies à proximité');
  await p.getByRole('button', { name: 'Voir les annonces' }).click();
  await calme(p);
  await capture(p, '01-explorer-rdc', 'Annonces de villas à Kinshasa, loyers en dollars');

  await p.getByRole('button', { name: 'Carte' }).click();
  await calme(p, 2500);
  await capture(p, '01-carte', 'Vue carte des annonces (OpenStreetMap, positions arrondies à 100 m)');

  // Pagination : « Voir plus d'annonces »
  await aller(p, '/explorer');
  await p.getByRole('button', { name: /Voir plus/ }).scrollIntoViewIfNeeded();
  await p.getByRole('button', { name: /Voir plus/ }).click();
  await calme(p);
  await p.mouse.wheel(0, 600);
  await calme(p);
  await capture(p, '01-pagination', 'Pagination : les annonces suivantes se chargent à la demande');

  const paris = Number(sql(`select b.id from biens b join bien_images i on i.bien_id = b.id
    where b.ville = 'Paris' and b.locataire_id is null and not b.archive group by b.id having count(*) >= 5 order by b.id limit 1`));
  await aller(p, `/annonces/${paris}`);
  await capture(p, '01-annonce-fr', 'Annonce en France : photos, loyer et charges, DPE');
  await capture(p, '01-annonce-fr-complete', 'Annonce complète : caractéristiques, conditions, adresse, propriétaire', { pleinePage: true });

  const villa = Number(sql(`select b.id from biens b join bien_images i on i.bien_id = b.id
    where b.type_bien = 'VILLA' and b.ville = 'Kinshasa' and b.locataire_id is null and not b.archive
    group by b.id having count(*) >= 4 order by b.id limit 1`));
  await aller(p, `/annonces/${villa}`);
  await capture(p, '01-annonce-rdc', 'Annonce à Kinshasa : commune, quartier, point de repère, équipements (forage, groupe…)');
  await capture(p, '01-annonce-rdc-complete', 'Annonce RDC complète : garantie et avance en mois, heures de courant', { pleinePage: true });

  await p.getByRole('button', { name: /Contacter/ }).click();
  await calme(p);
  await capture(p, '01-contacter-connexion', 'Contacter le propriétaire : connexion demandée');

  // Téléphone
  const t = await contexte('telephone');
  await aller(t, '/explorer');
  await capture(t, '01-m-explorer', 'Explorer sur téléphone');
  await aller(t, `/annonces/${villa}`);
  await capture(t, '01-m-annonce', 'Annonce sur téléphone');
  await t.context().close();
  await p.context().close();
}

// ───────────────────────────── 2. Inscription, e-mail de confirmation, mot de passe oublié ─────────────────────────────
async function inscription() {
  const email = 'thomas.roux@kupanga.demo';
  const p = await contexte('ordinateur');
  await aller(p, '/auth/register');
  await capture(p, '02-inscription-1', 'Inscription, étape 1 : trouver un logement ou mettre un bien en location');
  await p.getByRole('radio').first().click();
  await p.getByRole('button', { name: 'Continuer', exact: true }).click();
  await p.fill('#firstName', 'Thomas');
  await p.fill('#lastName', 'Roux');
  await p.fill('#telephone', '+33 6 45 67 89 12');
  await capture(p, '02-inscription-2', 'Inscription, étape 2 : identité et téléphone de contact (format international)');
  await p.getByRole('button', { name: 'Continuer', exact: true }).click();
  await p.fill('#email', email);
  await p.fill('#password', 'Thomas-Roux-2026');
  await p.fill('#confirmPassword', 'Thomas-Roux-2026');
  await capture(p, '02-inscription-3', 'Inscription, étape 3 : e-mail et mot de passe');
  await p.getByRole('button', { name: 'Créer mon compte' }).click();
  await p.getByText('Vérifiez votre e-mail').waitFor();
  await capture(p, '02-verifiez-email', 'Compte créé : un lien de confirmation part par e-mail');

  const m = await dernierEmail(email, 'confirm');
  await aller(p, 'http://localhost:8125/');
  await capture(p, '02-mailpit-boite', 'Mailpit : tous les e-mails envoyés par Kupanga');
  await captureEmail(p, m.ID, '02-email-confirmation', 'E-mail de confirmation de l’adresse');
  const lien = await lienDansEmail(m.ID, 'verifier-email');
  await aller(p, lien);
  await capture(p, '02-email-confirme', 'Adresse confirmée en un clic');

  // Mot de passe oublié → e-mail → nouveau mot de passe
  await aller(p, '/auth/forgot');
  await p.fill('#email', email);
  await capture(p, '02-mdp-oublie', 'Mot de passe oublié');
  await p.click('button[type=submit]');
  await calme(p);
  await capture(p, '02-mdp-oublie-envoye', 'Réponse identique que le compte existe ou non (pas d’énumération)');
  const r = await dernierEmail(email, 'mot de passe');
  await captureEmail(p, r.ID, '02-email-reinitialisation', 'E-mail de réinitialisation (lien valable 10 minutes)');
  await aller(p, await lienDansEmail(r.ID, 'reset-password'));
  await p.fill('#password', 'Kupanga-Demo-2026');
  await p.fill('#confirmPassword', 'Kupanga-Demo-2026');
  await capture(p, '02-nouveau-mdp', 'Choix du nouveau mot de passe');
  await p.click('button[type=submit]');
  await calme(p);

  await aller(p, '/auth/login');
  await p.fill('#email', email);
  await p.fill('#password', 'Kupanga-Demo-2026');
  await capture(p, '02-connexion', 'Connexion (e-mail et mot de passe, ou Google)');
  await connexion(p, email);
  await capture(p, '02-premier-accueil', 'Premier accueil : les premiers pas');
  await p.context().close();
}

// ───────────────────────────── 3. Propriétaire en France : accueil, biens, publier un bien ─────────────────────────────
const PHOTOS = (noms) => noms.map((n) => new URL(`../seed/photos/${n}`, import.meta.url).pathname.replace(/^\/([A-Z]:)/, '$1'));
import { readdirSync } from 'node:fs';
const photosDe = (cat, n) => readdirSync(new URL(`../seed/photos/${cat}/`, import.meta.url)).slice(0, n).map((f) => `${cat}/${f}`);

async function suivantAssistant(p) {
  await p.locator('kp-assistant button.kp-btn--primary').click();
  await calme(p, 500);
}

async function proprietaireFrance() {
  const p = await contexte('ordinateur');
  await connexion(p, C.claire);
  await aller(p, '/accueil');
  await capture(p, '03-accueil', 'Accueil de la propriétaire : tâches à faire (bail, état des lieux, fin de bail) et biens');
  await capture(p, '03-accueil-complet', 'Accueil : tâches à faire et aperçu des biens', { pleinePage: true });
  await aller(p, '/biens');
  await capture(p, '03-mes-biens', 'Mes biens : 26 biens, loués et vacants');
  await p.getByRole('radio', { name: /Loués/ }).or(p.getByRole('button', { name: /Loués/ })).first().click();
  await calme(p);
  await capture(p, '03-mes-biens-loues', 'Filtre : biens loués');

  // Publier un bien (assistant en 8 étapes)
  await aller(p, '/biens/nouveau');
  await p.getByRole('radio', { name: /France/ }).click();
  await capture(p, '03-nouveau-pays', 'Nouveau bien, étape 1 : le pays choisit les règles (devise, champs, modèle de bail)');
  await suivantAssistant(p);
  await p.getByRole('radio', { name: 'Appartement', exact: true }).click();
  await p.fill('#titre', 'Loft avec terrasse, Confluence');
  await p.fill('#description', 'Loft de 85 m² au dernier étage d’un ancien entrepôt réhabilité, terrasse de 20 m² plein ouest '
    + 'sur la Saône. Grande pièce de vie, cuisine ouverte équipée, deux chambres, salle d’eau. Tram T1 au pied de l’immeuble.');
  await capture(p, '03-nouveau-description', 'Étape 2 : type, titre et description');
  await suivantAssistant(p);
  await p.fill('#adresse', '24 quai Rambaud');
  await p.fill('#cp', '69002');
  await p.fill('#ville', 'Lyon');
  await capture(p, '03-nouveau-adresse', 'Étape 3 : adresse (code postal obligatoire en France)');
  await suivantAssistant(p);
  await calme(p, 2500);
  const carte = p.locator('[aria-label^="Carte"]').first();
  const boite = await carte.boundingBox();
  await p.mouse.click(boite.x + boite.width * 0.5, boite.y + boite.height * 0.5);
  await calme(p, 1500);
  await capture(p, '03-nouveau-carte', 'Étape 4 : repère placé sur la carte (ou GPS du téléphone)');
  await suivantAssistant(p);
  await p.fill('#surface', '85');
  await p.fill('#pieces', '3');
  await p.fill('#chambres', '2');
  await p.fill('#etage', '5');
  await p.locator('label.kp-check', { hasText: 'Ascenseur' }).click();
  await p.getByRole('button', { name: 'Parking', exact: true }).click().catch(() => {});
  await p.fill('#annee', '1910');
  await p.selectOption('#chauffage', { label: 'Pompe à chaleur' });
  await p.selectOption('#dpe', { label: 'B' });
  await p.selectOption('#ges', { label: 'A' });
  await capture(p, '03-nouveau-caracteristiques', 'Étape 5 : surface, pièces, équipements, DPE et GES');
  await suivantAssistant(p);
  await p.fill('#loyer', '1490');
  await p.fill('#charges', '110');
  await p.fill('#depot', '1490');
  await capture(p, '03-nouveau-loyer', 'Étape 6 : loyer, charges et dépôt de garantie en euros');
  await suivantAssistant(p);
  await p.locator('input[type=file]').setInputFiles(PHOTOS([...photosDe('immeuble', 1), ...photosDe('salon', 2),
    ...photosDe('cuisine', 1), ...photosDe('chambre', 1)]));
  await calme(p, 4000);
  await capture(p, '03-nouveau-photos', 'Étape 7 : photos (réduites dans le navigateur, métadonnées GPS retirées)');
  await suivantAssistant(p);
  await capture(p, '03-nouveau-recap', 'Étape 8 : récapitulatif avant publication');
  await suivantAssistant(p);
  await p.waitForURL((u) => !u.pathname.endsWith('/nouveau'), { timeout: 60000 });
  await calme(p, 1500);
  await aller(p, `/biens/${idBien('Loft avec terrasse, Confluence')}`);
  await capture(p, '03-bien-publie', 'Bien publié : fiche privée du propriétaire');
  await capture(p, '03-bien-publie-complet', 'Fiche du bien : documents, détails, équipements, localisation', { pleinePage: true });

  // Bien loué (Lucas) : locataire, contact, documents
  await aller(p, `/biens/${idBien('T3 lumineux avec balcon, Croix-Rousse')}`);
  await capture(p, '03-bien-loue', 'Bien loué : locataire, téléphone, message, WhatsApp, fin de bail');
  await capture(p, '03-bien-loue-complet', 'Bien loué : bail, état des lieux et quittances à jour', { pleinePage: true });
  await p.getByRole('button', { name: 'Actions sur le bien' }).click();
  await calme(p);
  await capture(p, '03-actions-bien', 'Actions : modifier, voir l’annonce, nouvelle quittance');
  await p.keyboard.press('Escape');

  await aller(p, '/documents');
  await capture(p, '03-documents', 'Documents : baux, états des lieux et quittances de tous les biens');
  await aller(p, '/notifications');
  await capture(p, '03-notifications', 'Notifications : signatures, paiements, congé, réservations');

  // Téléphone
  const t = await contexte('telephone');
  await connexion(t, C.claire);
  await aller(t, '/accueil');
  await capture(t, '03-m-accueil', 'Accueil propriétaire sur téléphone');
  await aller(t, '/biens');
  await capture(t, '03-m-biens', 'Mes biens sur téléphone');
  await t.context().close();
  await p.context().close();
}

// ───────────────────────────── 4. Louer : message, demande, réservation, bail, EDL, quittance ─────────────────────────────

/** Trace une signature manuscrite dans le premier canevas visible. */
async function dessinerSignature(p) {
  // Tracé cursif (boucles d'une cycloïde allongée) puis un trait de soulignement
  const c = p.locator('kp-signature canvas').last();
  await c.scrollIntoViewIfNeeded();
  const b = await c.boundingBox();
  const x0 = b.x + b.width * 0.14, y0 = b.y + b.height * 0.58, l = b.width * 0.62, r = b.height * 0.11;
  await p.mouse.move(x0, y0);
  await p.mouse.down();
  const n = 140, boucles = 7;
  for (let i = 0; i <= n; i++) {
    const t = i / n, a = 2 * Math.PI * boucles * t;
    const x = x0 + l * t - r * 0.9 * Math.sin(a);
    const y = y0 - r * (1 - Math.cos(a)) * (0.6 + 0.5 * Math.sin(Math.PI * t)) + Math.sin(t * 9) * r * 0.25;
    await p.mouse.move(x, y);
  }
  await p.mouse.up();
  await p.mouse.move(x0 - b.width * 0.02, y0 + r * 1.3);
  await p.mouse.down();
  for (let i = 0; i <= 20; i++) await p.mouse.move(x0 + l * 1.05 * (i / 20), y0 + r * 1.3 - Math.sin(i / 20 * Math.PI) * r * 0.5);
  await p.mouse.up();
  await p.waitForTimeout(300);
}

async function location() {
  const loft = idBien('Loft avec terrasse, Confluence');
  const claire = await contexte('ordinateur');
  const nadia = await contexte('ordinateur');
  await connexion(claire, C.claire);
  await connexion(nadia, C.nadia);

  // Premier contact depuis l'annonce, reçu en temps réel par la propriétaire
  await aller(nadia, `/annonces/${loft}`);
  await capture(nadia, '04-annonce-loft', 'L’annonce publiée, vue par une candidate');
  await nadia.getByRole('button', { name: /Contacter/ }).click();
  await calme(nadia);
  await aller(claire, '/messages');
  await nadia.fill('#message', 'Bonjour Claire, votre loft m’intéresse beaucoup. Serait-il possible de le visiter samedi ?');
  await capture(nadia, '04-premier-message', 'Premier message au propriétaire, sans connaître son e-mail');
  await nadia.getByRole('button', { name: 'Envoyer' }).click();
  await calme(claire, 2500);
  await capture(claire, '04-message-recu', 'Messagerie temps réel (WebSocket) : le message arrive instantanément');
  await claire.getByText('Nadia', { exact: false }).first().click();
  await calme(claire);
  await claire.fill('#message', 'Bonjour Nadia, avec plaisir ! Samedi 10 h, au 24 quai Rambaud.');
  await claire.getByRole('button', { name: 'Envoyer' }).click();
  await calme(nadia, 2500);
  await capture(claire, '04-conversation', 'Conversation propriétaire ↔ candidate, rattachée au bien');
  await capture(nadia, '04-conversation-candidate', 'Réponse reçue côté candidate');

  // Proposer la location
  await aller(claire, `/biens/${loft}/locataire`);
  await capture(claire, '04-proposer', 'Proposer la location à l’une des personnes qui ont écrit pour ce bien');
  await claire.getByRole('button', { name: /Nadia/ }).click();
  await calme(claire);
  await capture(claire, '04-proposer-confirmer', 'Confirmation : la candidate reçoit une notification et un e-mail (72 h pour répondre)');
  await claire.locator('kp-sheet button.kp-btn--primary, .kp-dialog button.kp-btn--primary').last().click();
  await calme(claire);
  await capture(claire, '04-demande-envoyee', 'Demande envoyée, en attente de réponse');
  const demande = await dernierEmail(C.nadia, 'propos');
  await captureEmail(claire, demande.ID, '04-email-demande', 'E-mail « un propriétaire vous propose son logement »');

  await aller(nadia, '/demandes-location');
  await capture(nadia, '04-demande-recue', 'La candidate accepte ou refuse la proposition');
  await nadia.getByRole('button', { name: 'Accepter' }).first().click();
  await calme(nadia);
  await capture(nadia, '04-reservation', 'Logement réservé : 14 jours pour signer le bail (désistement possible)');
  await aller(claire, `/biens/${loft}`);
  await capture(claire, '04-bien-reserve', 'Côté propriétaire : bien réservé, annonce retirée de la recherche');

  // Bail
  await aller(claire, `/biens/${loft}/bail/nouveau`);
  await claire.getByRole('button', { name: /3 ans/ }).click().catch(() => {});
  await capture(claire, '04-bail-1', 'Nouveau bail : date de début et durée');
  await suivantAssistant(claire);
  await capture(claire, '04-bail-2', 'Loyer, charges et dépôt de garantie (repris de l’annonce)');
  await suivantAssistant(claire);
  await capture(claire, '04-bail-3', 'Récapitulatif du bail');
  await suivantAssistant(claire);
  await claire.waitForURL(/\/bail$/, { timeout: 30000 }).catch(() => {});
  await calme(claire);
  await capture(claire, '04-bail-cree', 'Bail créé : à signer par le propriétaire puis par le locataire');
  await claire.getByRole('button', { name: /Signer le bail/ }).click();
  await calme(claire);
  await dessinerSignature(claire);
  await capture(claire, '04-bail-signature-proprio', 'Signature manuscrite à l’écran (souris, doigt ou stylet)');
  await claire.locator('kp-feuille-signature button.kp-btn--primary').last().click();
  await calme(claire, 1500);
  await capture(claire, '04-bail-attente-locataire', 'En attente du locataire : lien envoyé par e-mail, relance WhatsApp possible');

  const mailBail = await dernierEmail(C.nadia, 'contrat');
  await captureEmail(claire, mailBail.ID, '04-email-bail', 'E-mail au locataire : bail à signer en ligne (PDF joint)');
  const lienBail = await lienDansEmail(mailBail.ID, 'contrats/signer/');
  const tel = await contexte('telephone');
  await connexion(tel, C.nadia);
  await aller(tel, lienBail);
  await capture(tel, '04-m-bail-a-signer', 'Le locataire relit le bail sur son téléphone');
  await capture(tel, '04-m-bail-a-signer-complet', 'Signature du bail : conditions, lecture du bail complet, signature', { pleinePage: true });
  await dessinerSignature(tel);
  await capture(tel, '04-m-bail-signature', 'Signature du bail au doigt');
  await tel.getByRole('button', { name: /Signer/ }).last().click();
  await calme(tel, 2000);
  await capture(tel, '04-m-bail-signe', 'Bail signé par les deux parties');
  await aller(claire, `/biens/${loft}/bail`);
  await capture(claire, '04-bail-signe', 'Bail signé : le PDF définitif est envoyé aux deux parties');
  await aller(claire, `/biens/${loft}`);
  await capture(claire, '04-bien-loue', 'Le candidat devient locataire à la signature');

  // État des lieux d'entrée
  await aller(claire, `/biens/${loft}/etats-des-lieux/nouveau`);
  await capture(claire, '04-edl-1', 'État des lieux d’entrée : date et heure');
  await suivantAssistant(claire);
  const releves = [['23518', 'PDL 1422 5938 1107'], ['1874', 'GRDF 0 554 218'], ['412', 'EF-77120']];
  for (const [i, [index, num]] of releves.entries()) {
    if (await claire.locator(`#index-${i}`).count()) {
      await claire.fill(`#index-${i}`, index);
      await claire.fill(`#num-${i}`, num);
    }
  }
  await capture(claire, '04-edl-2', 'Relevé des compteurs');
  await suivantAssistant(claire);
  for (const cle of ['Porte d’entrée', 'Boîte aux lettres']) await claire.getByRole('button', { name: cle }).click().catch(() => {});
  await capture(claire, '04-edl-3', 'Clés remises');
  await suivantAssistant(claire);
  for (const piece of ['Entrée', 'Salon', 'Cuisine', 'Chambre', 'Salle de bain']) {
    await claire.getByRole('button', { name: piece, exact: false }).first().click().catch(() => {});
  }
  await calme(claire);
  await capture(claire, '04-edl-4', 'Pièce par pièce : murs, sols, fenêtres… et leur état');
  await suivantAssistant(claire);
  await claire.fill('#observations', 'Logement remis propre. Terrasse nettoyée, store fonctionnel.').catch(() => {});
  await capture(claire, '04-edl-5', 'Observations et récapitulatif');
  await suivantAssistant(claire);
  await calme(claire, 1500);
  await capture(claire, '04-edl-cree', 'État des lieux créé, à signer');
  await claire.getByRole('button', { name: /Signer/ }).first().click().catch(() => {});
  await calme(claire);
  if (await claire.locator('kp-signature canvas').count()) {
    await dessinerSignature(claire);
    await claire.locator('kp-feuille-signature button.kp-btn--primary').last().click();
    await calme(claire, 1500);
  }
  const mailEdl = await dernierEmail(C.nadia, 'état des lieux');
  await aller(tel, await lienDansEmail(mailEdl.ID, 'edl/signer/'));
  await capture(tel, '04-m-edl-a-signer', 'Le locataire relit l’état des lieux pièce par pièce');
  await dessinerSignature(tel);
  await tel.getByRole('button', { name: /Signer/ }).last().click();
  await calme(tel, 2000);
  await capture(tel, '04-m-edl-signe', 'État des lieux signé');

  // Quittance
  await aller(claire, `/biens/${loft}/quittances/nouvelle`);
  await capture(claire, '04-quittance-1', 'Nouvelle quittance : mois, échéance');
  await suivantAssistant(claire);
  await capture(claire, '04-quittance-2', 'Montants repris du bail');
  await suivantAssistant(claire);
  await calme(claire, 1500);
  await aller(claire, `/biens/${loft}/quittances`);
  await capture(claire, '04-quittances', 'Quittances du bien : en attente de paiement');
  await claire.locator('button.kp-btn--primary', { hasText: /reçu|payé/i }).first().click();
  await calme(claire);
  await claire.getByRole('radio', { name: 'Virement' }).click().catch(() => {});
  await claire.fill('#reference', 'VIR-NB-1600').catch(() => {});
  if (await claire.locator('kp-signature canvas').count()) await dessinerSignature(claire);
  await capture(claire, '04-loyer-recu', 'Déclarer le loyer reçu hors application : mode, date, référence, signature');
  await claire.locator('kp-sheet button.kp-btn--primary').last().click();
  await calme(claire, 2000);
  await capture(claire, '04-quittance-payee', 'Quittance payée : PDF envoyé au locataire, reçu joignable');
  const mailQ = await dernierEmail(C.nadia, 'quittance');
  await captureEmail(claire, mailQ.ID, '04-email-quittance', 'E-mail de quittance au locataire (PDF joint)');

  // Côté locataire
  await aller(tel, '/mon-logement');
  await capture(tel, '04-m-mon-logement', 'Mon logement : loyer, bail, propriétaire, documents');
  await aller(tel, '/quittances');
  await capture(tel, '04-m-quittances', 'Mes quittances sur téléphone');
  await aller(nadia, '/mon-logement');
  await capture(nadia, '04-mon-logement', 'Mon logement (ordinateur)');
  await capture(nadia, '04-mon-logement-complet', 'Mon logement : loyer, bail, propriétaire, documents', { pleinePage: true });

  for (const x of [tel, nadia, claire]) await x.context().close();
}

// ───────────────────────────── 5. Fin de bail : congé, sortie, dépôt restitué ─────────────────────────────
async function finDeBail() {
  const lucas = await contexte('ordinateur');
  await connexion(lucas, C.lucas);
  await aller(lucas, '/mon-logement');
  await lucas.getByRole('button', { name: /Donner congé/ }).click();
  await calme(lucas);
  await capture(lucas, '05-donner-conge', 'Le locataire (ou le propriétaire) donne congé avec une date de fin');
  await lucas.context().close();

  const claire = await contexte('ordinateur');
  await connexion(claire, C.claire);
  await aller(claire, '/accueil');
  await capture(claire, '05-accueil-fin', 'Accueil : fin de bail à enregistrer');
  const t2 = idBien('T2 rénové, Brotteaux');
  await aller(claire, `/biens/${t2}`);
  await capture(claire, '05-conge-recu', 'Congé reçu : date de fin annoncée');
  await claire.getByRole('button', { name: /Enregistrer la fin/ }).click();
  await calme(claire);
  await claire.fill('#depot', '790');
  await claire.fill('#motif', 'Remplacement de la plaque de cuisson rayée (facture jointe par e-mail).');
  await capture(claire, '05-fin-de-bail', 'Fin du bail : dépôt restitué, motif obligatoire en cas de retenue');
  await claire.locator('kp-sheet button.kp-btn--primary').last().click();
  await calme(claire, 1500);
  await capture(claire, '05-bien-libere', 'Bien libéré : de nouveau vacant et dans les annonces');
  const m = await dernierEmail(C.sophie, 'fin');
  await captureEmail(claire, m.ID, '05-email-fin-de-bail', 'E-mail de fin de bail au locataire (dépôt restitué)');
  await claire.context().close();
}

// ───────────────────────────── 6. RDC : Kinshasa, dollars, repère, WhatsApp, Mobile Money ─────────────────────────────
import { writeFileSync as ecrire, readFileSync as lire, existsSync as existe } from 'node:fs';
import { SORTIE } from './outils.mjs';

/** Lien WhatsApp (wa.me) généré par l'API : message décodé gardé pour la vidéo, page WhatsApp capturée si possible. */
async function lienWhatsapp(p, nom, description) {
  const a = p.locator('a.kp-btn--whatsapp').first();
  await a.waitFor({ timeout: 15000 });
  const url = await a.getAttribute('href');
  const fichier = `${SORTIE}/whatsapp.json`;
  const liens = existe(fichier) ? JSON.parse(lire(fichier, 'utf8')) : {};
  const u = new URL(url);
  liens[nom] = { description, url, numero: u.pathname.replace('/', ''), texte: u.searchParams.get('text') };
  ecrire(fichier, JSON.stringify(liens, null, 1));
  await a.scrollIntoViewIfNeeded();
  await a.hover();
  await capture(p, nom, description);
}

async function rdc() {
  const p = await contexte('ordinateur');
  await p.context().setGeolocation({ latitude: -4.3215, longitude: 15.2905 });
  await connexion(p, C.patrick);
  await aller(p, '/accueil');
  await capture(p, '06-accueil', 'Accueil de Patrick, propriétaire à Kinshasa');
  await aller(p, '/biens');
  await capture(p, '06-mes-biens', 'Ses biens en RDC : villas, annexes, appartements, loyers en USD ou CDF');

  await aller(p, '/biens/nouveau');
  await p.getByRole('radio', { name: /Congo/ }).click();
  await capture(p, '06-nouveau-pays', 'Pays : République démocratique du Congo');
  await suivantAssistant(p);
  await p.getByRole('radio', { name: 'Villa', exact: true }).click();
  await p.fill('#titre', 'Villa 3 chambres avec jardin, Lingwala');
  await p.fill('#description', 'Villa de plain-pied dans une parcelle clôturée et arborée : trois chambres, salon climatisé, '
    + 'cuisine équipée, dépendance. Forage, groupe électrogène et gardien. À cinq minutes du boulevard du 30 Juin.');
  await capture(p, '06-nouveau-type', 'Types de bien congolais : villa, annexe, chambre, parcelle, local commercial…');
  await suivantAssistant(p);
  await p.fill('#adresse', 'n° 52, avenue Lokole');
  await p.fill('#parcelle', '1184');
  await p.fill('#quartier', 'Lokole');
  await p.fill('#commune', 'Lingwala');
  await p.fill('#ville', 'Kinshasa');
  await p.fill('#repere', 'En face de l’école Saint-Georges, portail vert');
  await capture(p, '06-nouveau-adresse', 'Adresse congolaise : commune, quartier, parcelle et point de repère (pas de code postal)');
  await suivantAssistant(p);
  await calme(p, 2000);
  await p.getByRole('button', { name: /Ma position/ }).click();
  await calme(p, 2500);
  await capture(p, '06-nouveau-gps', 'Repère placé avec le GPS du téléphone : pas besoin d’une adresse géocodable');
  await suivantAssistant(p);
  await p.fill('#surface', '240');
  await p.fill('#pieces', '6');
  await p.fill('#chambres', '3');
  for (const e of ['Forage', 'Groupe électrogène', 'Clôture', 'Gardiennage', 'Climatisation']) {
    await p.getByRole('button', { name: new RegExp(e) }).first().click().catch(() => {});
  }
  await p.fill('#heures', '18');
  await capture(p, '06-nouveau-equipements', 'Équipements utiles à Kinshasa et heures de courant par jour');
  await suivantAssistant(p);
  await p.fill('#loyer', '1800');
  await p.fill('#garantie', '3');
  await p.fill('#avance', '6');
  await capture(p, '06-nouveau-loyer', 'Loyer en dollars, garantie et loyers d’avance comptés en mois');
  await suivantAssistant(p);
  await p.locator('input[type=file]').setInputFiles(PHOTOS([...photosDe('villa', 2), ...photosDe('salon', 1), ...photosDe('chambre', 1)]));
  await calme(p, 4000);
  await suivantAssistant(p);
  await capture(p, '06-nouveau-recap', 'Récapitulatif avant publication');
  await suivantAssistant(p);
  await p.waitForURL((u) => !u.pathname.endsWith('/nouveau'), { timeout: 60000 });
  await aller(p, `/annonces/${idBien('Villa 3 chambres avec jardin, Lingwala')}`);
  await capture(p, '06-annonce-publiee', 'Annonce publiée : repère arrondi à 100 m pour les visiteurs');

  // Villa louée à Grâce
  const villa = idBien('Villa 4 chambres avec piscine, Ma Campagne');
  await aller(p, `/biens/${villa}`);
  await capture(p, '06-villa-louee', 'Villa louée : locataire, téléphone +243, WhatsApp');
  await capture(p, '06-villa-louee-complet', 'Villa louée : documents, garantie et avance, équipements', { pleinePage: true });
  await aller(p, `/biens/${villa}/bail`);
  await capture(p, '06-bail-rdc', 'Bail RDC signé : garantie de 3 mois et 6 mois d’avance, en dollars');
  await aller(p, `/biens/${villa}/etats-des-lieux`);
  await capture(p, '06-edl-rdc', 'État des lieux : compteur Cash Power, REGIDESO et groupe électrogène');
  await aller(p, `/biens/${villa}/quittances`);
  await capture(p, '06-quittances-rdc', 'Quittances : loyer payé par Mobile Money (M-Pesa), reçu photographié');
  await lienWhatsapp(p, '06-whatsapp-rappel', 'Rappel de loyer par WhatsApp : message prérempli, envoyé depuis le téléphone du propriétaire');

  const appart = idBien('Appartement 3 pièces, Gombe');
  await aller(p, `/biens/${appart}/locataire`);
  await lienWhatsapp(p, '06-whatsapp-demande', 'Demande de location envoyée, à signaler aussi par WhatsApp');

  // Téléphone de Grâce
  const t = await contexte('telephone');
  await connexion(t, C.grace);
  await aller(t, '/mon-logement');
  await capture(t, '06-m-mon-logement', 'Grâce, locataire à Kinshasa : son logement sur téléphone');
  await aller(t, '/quittances');
  await capture(t, '06-m-quittances', 'Ses quittances en dollars');
  await t.context().close();
  await p.context().close();
}

// ───────────────────────────── 7. Compte, thème sombre, notifications, documents du locataire ─────────────────────────────
async function compte() {
  const p = await contexte('ordinateur');
  await connexion(p, C.lucas);
  await aller(p, '/compte');
  await capture(p, '07-compte', 'Compte : téléphone, thème, notifications, déconnexion');
  await aller(p, '/documents');
  await capture(p, '07-documents-locataire', 'Documents du locataire : bail, état des lieux, quittances');
  await aller(p, '/notifications');
  await capture(p, '07-notifications-locataire', 'Notifications du locataire');
  await p.context().close();

  const s = await contexte('ordinateur', { sombre: true });
  await connexion(s, C.claire);
  await aller(s, '/accueil');
  await capture(s, '07-sombre-accueil', 'Thème sombre (selon le système ou au choix)');
  await aller(s, '/explorer');
  await capture(s, '07-sombre-explorer', 'Explorer en thème sombre');
  await s.context().close();

  const t = await contexte('telephone', { sombre: true });
  await connexion(t, C.grace);
  await aller(t, '/mon-logement');
  await capture(t, '07-m-sombre-logement', 'Thème sombre sur téléphone');
  await t.context().close();

}

// ───────────────────────────── 8. Back-office ─────────────────────────────
async function backoffice() {
  const API_BO = 'http://localhost:8189/backoffice';
  const p = await contexte('ordinateur');
  await aller(p, `${API_BO}/login`);
  await p.fill('#username', 'admin@kupanga.demo');
  await p.fill('#password', 'Admin-Demo-2026');
  await capture(p, '08-bo-login', 'Back-office : connexion de l’administrateur (session séparée de l’API)');
  await p.locator('button[type=submit]').click();
  await calme(p, 1500);
  await capture(p, '08-bo-tableau-de-bord', 'Tableau de bord : biens, comptes, documents, e-mails en échec');
  await aller(p, `${API_BO}/analytics`);
  await calme(p, 1500);
  await capture(p, '08-bo-statistiques', 'Statistiques : inscriptions, publications, répartition par pays et par type');
  await capture(p, '08-bo-statistiques-complet', 'Statistiques : inscriptions, publications, pays et types de biens', { pleinePage: true });
  await aller(p, `${API_BO}/biens`);
  await capture(p, '08-bo-biens', 'Tous les biens (745), paginés');
  await aller(p, `${API_BO}/biens?pays=CD&typeBien=VILLA&page=0&size=20`);
  await capture(p, '08-bo-biens-filtres', 'Filtres : pays, ville, titre, type');
  await aller(p, `${API_BO}/biens?page=12&size=20`);
  await capture(p, '08-bo-biens-page', 'Pagination : page 13');
  const archiver = p.locator('button[title="Archiver ce bien"]').first();
  if (await archiver.count()) {
    await archiver.click();
    await calme(p);
    await capture(p, '08-bo-archiver', 'Archiver un bien : jamais supprimé, baux conservés');
    await p.keyboard.press('Escape');
  }
  const villa = idBien('Villa 4 chambres avec piscine, Ma Campagne');
  await aller(p, `${API_BO}/biens/${villa}/documents`);
  await capture(p, '08-bo-documents', 'Documents d’un bien : baux, états des lieux, quittances');
  await aller(p, `${API_BO}/users`);
  await capture(p, '08-bo-comptes', 'Comptes : propriétaires, locataires, non confirmés, anonymisés');
  const supprimer = p.locator('button[title="Supprimer"]').first();
  if (await supprimer.count()) {
    await supprimer.click();
    await calme(p);
    await capture(p, '08-bo-supprimer', 'Supprimer un compte : supprimé ou anonymisé s’il a des documents (RGPD)');
    await p.keyboard.press('Escape');
  }
  await aller(p, `${API_BO}/emails`);
  await capture(p, '08-bo-emails', 'File des e-mails : envoyés, en attente, en échec, renvoi possible');
  await p.context().close();
}

// ───────────────────────────── 9. Mailpit : vue d'ensemble ─────────────────────────────
async function mailpit() {
  const p = await contexte('ordinateur');
  await aller(p, 'http://localhost:8125/');
  await capture(p, '09-mailpit', 'Tous les e-mails envoyés pendant la démo');
  const m = await dernierEmail(C.claire, 'congé').catch(() => null);
  if (m) await captureEmail(p, m.ID, '09-email-conge', 'E-mail : congé reçu');
  const r = await dernierEmail(C.grace, 'quittance').catch(() => null);
  if (r) await captureEmail(p, r.ID, '09-email-quittance-rdc', 'E-mail : quittance en dollars');
  await p.context().close();
}

const SEGMENTS = { visiteur, inscription, proprietaireFrance, location, finDeBail, rdc, compte, backoffice, mailpit };

const demandes = process.argv.slice(2);
for (const nom of demandes.length ? demandes : Object.keys(SEGMENTS)) {
  console.log('▶', nom);
  await SEGMENTS[nom]();
}
await fin();
void FRONT; void connexion; void dernierEmail; void lienDansEmail; void captureEmail; void idBien; void C;
