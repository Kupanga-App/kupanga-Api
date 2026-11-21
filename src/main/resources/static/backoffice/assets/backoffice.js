/* Back-office Kupanga : icônes, thème, menu du téléphone, fenêtres de confirmation, galeries de photos. */
(function () {
  'use strict';

  if (window.lucide) {
    window.lucide.createIcons();
  }

  var racine = document.documentElement;

  /* ── Thème : bascule clair / sombre, mémorisée ; sans choix, celui de l'appareil ── */
  function themeEffectif() {
    if (racine.dataset.theme === 'light' || racine.dataset.theme === 'dark') return racine.dataset.theme;
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.querySelectorAll('[data-kp-theme]').forEach(function (bouton) {
    bouton.addEventListener('click', function () {
      var suivant = themeEffectif() === 'dark' ? 'light' : 'dark';
      racine.dataset.theme = suivant;
      try { localStorage.setItem('kupanga-admin-theme', suivant); } catch (e) { /* stockage indisponible */ }
      document.dispatchEvent(new CustomEvent('kp-theme'));
    });
  });

  /* ── Menu du téléphone ── */
  var boutonMenu = document.querySelector('[data-kp-menu]');
  function menu(ouvert) {
    document.body.classList.toggle('kp-menu-ouvert', ouvert);
    if (boutonMenu) boutonMenu.setAttribute('aria-expanded', String(ouvert));
  }
  if (boutonMenu) boutonMenu.addEventListener('click', function () { menu(!document.body.classList.contains('kp-menu-ouvert')); });
  document.querySelectorAll('[data-kp-menu-fermer]').forEach(function (el) { el.addEventListener('click', function () { menu(false); }); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') menu(false); });

  /* ── Confirmation : <button data-kp-confirmer="#id-dialog" data-action="/url" data-nom="…"> ── */
  document.querySelectorAll('[data-kp-confirmer]').forEach(function (bouton) {
    bouton.addEventListener('click', function () {
      var dialog = document.querySelector(bouton.dataset.kpConfirmer);
      if (!dialog) return;
      var form = dialog.querySelector('form');
      if (form && bouton.dataset.action) form.setAttribute('action', bouton.dataset.action);
      var nom = dialog.querySelector('[data-kp-nom]');
      if (nom) nom.textContent = bouton.dataset.nom || '';
      if (typeof dialog.showModal === 'function') dialog.showModal();
    });
  });
  document.querySelectorAll('[data-kp-fermer]').forEach(function (bouton) {
    bouton.addEventListener('click', function () { var d = bouton.closest('dialog'); if (d) d.close(); });
  });
  document.querySelectorAll('dialog.kp-dialog').forEach(function (dialog) {
    dialog.addEventListener('click', function (e) { if (e.target === dialog) dialog.close(); });
  });

  /* ── Galeries de photos (défilement horizontal) ── */
  document.querySelectorAll('[data-kp-galerie]').forEach(function (wrap) {
    var piste = wrap.querySelector('.kp-galerie');
    var compteur = wrap.querySelector('[data-kp-compteur]');
    var total = piste ? piste.querySelectorAll('img').length : 0;
    function position() { return piste.clientWidth ? Math.round(piste.scrollLeft / piste.clientWidth) : 0; }
    function maj() { if (compteur) compteur.textContent = (position() + 1) + ' / ' + total; }
    if (!piste || total === 0) return;
    piste.addEventListener('scroll', function () { window.requestAnimationFrame(maj); }, { passive: true });
    wrap.querySelectorAll('[data-kp-sens]').forEach(function (b) {
      b.addEventListener('click', function () {
        var i = position() + Number(b.dataset.kpSens);
        if (i < 0) i = total - 1;
        if (i >= total) i = 0;
        piste.scrollTo({ left: i * piste.clientWidth, behavior: 'smooth' });
      });
    });
    maj();
  });
})();
