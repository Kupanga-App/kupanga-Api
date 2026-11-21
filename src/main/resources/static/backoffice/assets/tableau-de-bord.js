/*
 * Tableau de bord du back-office : graphiques en SVG, sans bibliothèque.
 * Données : window.KP_MOIS (12 mois, du plus ancien au mois en cours), écrit par dashboard.html.
 * Couleurs : variables --kp-serie-* (validées pour le daltonisme, clair et sombre), donc le thème suit sans redessiner.
 * Repères : un seul axe, traits de 2 px, points de 8 px cerclés de la couleur de la surface, colonnes de 24 px au plus,
 * 2 px de vide entre segments, coins arrondis seulement en haut, bulle au survol, tableau des données sous chaque graphique.
 */
(function () {
  'use strict';

  var MOIS = window.KP_MOIS || [];
  if (!MOIS.length) return;

  var NS = 'http://www.w3.org/2000/svg';
  var MARGE = { haut: 12, droite: 44, bas: 28, gauche: 36 };

  function el(nom, attrs, parent) {
    var n = document.createElementNS(NS, nom);
    Object.keys(attrs || {}).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    if (parent) parent.appendChild(n);
    return n;
  }

  var format = new Intl.NumberFormat('fr-FR');

  /* Graduations rondes : 0, 5, 10… ou 0, 20, 40… ; au moins 4 si possible */
  function graduations(max) {
    if (max <= 0) return { max: 4, pas: 1 };
    var brut = max / 4;
    var puissance = Math.pow(10, Math.floor(Math.log10(brut)));
    var pas = [1, 2, 2.5, 5, 10].map(function (m) { return m * puissance; }).find(function (p) { return p >= brut; });
    pas = Math.max(1, Math.ceil(pas));
    return { max: Math.ceil(max / pas) * pas, pas: pas };
  }

  function preparer(conteneur) {
    conteneur.innerHTML = '';
    var largeur = conteneur.clientWidth;
    var hauteur = conteneur.clientHeight;
    var svg = el('svg', { viewBox: '0 0 ' + largeur + ' ' + hauteur, 'aria-hidden': 'true', focusable: 'false' }, conteneur);
    var bulle = document.createElement('div');
    bulle.className = 'kp-bulle';
    conteneur.appendChild(bulle);
    return {
      svg: svg, bulle: bulle, largeur: largeur, hauteur: hauteur,
      x0: MARGE.gauche, x1: largeur - MARGE.droite, y0: hauteur - MARGE.bas, y1: MARGE.haut
    };
  }

  function axes(g, echelle) {
    var y = function (v) { return g.y0 - (v / echelle.max) * (g.y0 - g.y1); };
    for (var v = 0; v <= echelle.max; v += echelle.pas) {
      el('line', { x1: g.x0, x2: g.x1, y1: Math.round(y(v)) + 0.5, y2: Math.round(y(v)) + 0.5,
        class: v === 0 ? 'kp-g-repere' : 'kp-g-grille' }, g.svg);
      var t = el('text', { x: g.x0 - 8, y: y(v) + 4, 'text-anchor': 'end' }, g.svg);
      t.textContent = format.format(v);
    }
    return y;
  }

  function etiquettesMois(g, xDe) {
    var pas = (g.x1 - g.x0) / MOIS.length < 38 ? 2 : 1;
    MOIS.forEach(function (m, i) {
      // Un mois sur deux si l'espace manque, toujours le dernier (mois en cours)
      if ((MOIS.length - 1 - i) % pas !== 0) return;
      var t = el('text', { x: xDe(i), y: g.hauteur - 8, 'text-anchor': 'middle' }, g.svg);
      t.textContent = m.libelle;
    });
  }

  function afficherBulle(g, titre, lignes, x) {
    var html = '<div class="kp-bulle__titre"></div>';
    lignes.forEach(function () {
      html += '<div class="kp-bulle__ligne"><span class="kp-bulle__point"></span><span></span><b></b></div>';
    });
    g.bulle.innerHTML = html;
    g.bulle.querySelector('.kp-bulle__titre').textContent = titre;
    g.bulle.querySelectorAll('.kp-bulle__ligne').forEach(function (div, i) {
      div.querySelector('.kp-bulle__point').style.setProperty('--_c', 'var(' + lignes[i].couleur + ')');
      div.children[1].textContent = lignes[i].nom;
      div.children[2].textContent = format.format(lignes[i].valeur);
    });
    var largeur = g.bulle.offsetWidth || 160;
    var gauche = x + 14;
    if (gauche + largeur > g.largeur) gauche = x - largeur - 14;
    g.bulle.style.left = Math.max(0, gauche) + 'px';
    g.bulle.style.top = MARGE.haut + 'px';
    g.bulle.classList.add('visible');
  }

  function masquerBulle(g) { g.bulle.classList.remove('visible'); }

  /* Zones de survol : une colonne transparente par mois, plus large que la marque */
  function zonesSurvol(g, xDe, largeurZone, auSurvol, aLaSortie) {
    MOIS.forEach(function (m, i) {
      var r = el('rect', { x: xDe(i) - largeurZone / 2, y: g.y1, width: largeurZone, height: g.y0 - g.y1,
        fill: 'transparent' }, g.svg);
      r.addEventListener('mouseenter', function () { auSurvol(i); });
      r.addEventListener('mouseleave', aLaSortie);
    });
  }

  /* ── Courbes : inscriptions et biens publiés ── */
  function courbes(conteneur) {
    var series = [
      { cle: 'inscriptions', nom: 'Inscriptions', couleur: '--kp-serie-1' },
      { cle: 'biens', nom: 'Biens publiés', couleur: '--kp-serie-2' }
    ];
    var g = preparer(conteneur);
    var max = 0;
    MOIS.forEach(function (m) { series.forEach(function (s) { max = Math.max(max, m[s.cle]); }); });
    var echelle = graduations(max);
    var y = axes(g, echelle);
    var pas = (g.x1 - g.x0 - 16) / (MOIS.length - 1);
    var xDe = function (i) { return g.x0 + 8 + i * pas; };
    etiquettesMois(g, xDe);

    var repere = el('line', { y1: g.y1, y2: g.y0, class: 'kp-g-repere', visibility: 'hidden' }, g.svg);

    series.forEach(function (s) {
      var d = MOIS.map(function (m, i) { return (i ? 'L' : 'M') + xDe(i).toFixed(1) + ' ' + y(m[s.cle]).toFixed(1); }).join(' ');
      el('path', { d: d, fill: 'none', 'stroke-width': 2, 'stroke-linejoin': 'round', 'stroke-linecap': 'round',
        style: 'stroke: var(' + s.couleur + ')' }, g.svg);
    });

    // Valeur au bout de chaque courbe (étiquette sélective), décalée si les deux se touchent
    var fin = MOIS.length - 1;
    var bouts = series.map(function (s) { return { s: s, y: y(MOIS[fin][s.cle]) }; });
    if (Math.abs(bouts[0].y - bouts[1].y) < 14) {
      var haut = bouts[0].y <= bouts[1].y ? 0 : 1;
      bouts[haut].y -= 7; bouts[1 - haut].y += 7;
    }
    series.forEach(function (s, k) {
      el('circle', { cx: xDe(fin), cy: y(MOIS[fin][s.cle]), r: 4, 'stroke-width': 2,
        style: 'fill: var(' + s.couleur + '); stroke: var(--kp-surface)' }, g.svg);
      var t = el('text', { x: xDe(fin) + 10, y: bouts[k].y + 4, style: 'fill: var(--kp-text); font-weight: 600' }, g.svg);
      t.textContent = format.format(MOIS[fin][s.cle]);
    });

    var points = series.map(function (s) {
      return el('circle', { r: 4, 'stroke-width': 2, visibility: 'hidden',
        style: 'fill: var(' + s.couleur + '); stroke: var(--kp-surface)' }, g.svg);
    });

    zonesSurvol(g, xDe, Math.max(pas, 16), function (i) {
      repere.setAttribute('x1', xDe(i)); repere.setAttribute('x2', xDe(i)); repere.setAttribute('visibility', 'visible');
      series.forEach(function (s, k) {
        points[k].setAttribute('cx', xDe(i)); points[k].setAttribute('cy', y(MOIS[i][s.cle]));
        points[k].setAttribute('visibility', 'visible');
      });
      afficherBulle(g, MOIS[i].libelleLong, series.map(function (s) {
        return { nom: s.nom, couleur: s.couleur, valeur: MOIS[i][s.cle] };
      }), xDe(i));
    }, function () {
      repere.setAttribute('visibility', 'hidden');
      points.forEach(function (p) { p.setAttribute('visibility', 'hidden'); });
      masquerBulle(g);
    });
  }

  /* ── Colonnes empilées : baux, états des lieux, quittances ── */
  function colonnes(conteneur) {
    var series = [
      { cle: 'baux', nom: 'Baux signés', couleur: '--kp-serie-1' },
      { cle: 'edl', nom: 'États des lieux signés', couleur: '--kp-serie-2' },
      { cle: 'quittances', nom: 'Quittances émises', couleur: '--kp-serie-3' }
    ];
    var g = preparer(conteneur);
    var totaux = MOIS.map(function (m) { return series.reduce(function (t, s) { return t + m[s.cle]; }, 0); });
    var echelle = graduations(Math.max.apply(null, totaux));
    var y = axes(g, echelle);
    var bande = (g.x1 - g.x0) / MOIS.length;
    var largeur = Math.min(24, bande * 0.6);
    var xDe = function (i) { return g.x0 + bande * (i + 0.5); };
    etiquettesMois(g, xDe);

    var colonnesSvg = [];
    MOIS.forEach(function (m, i) {
      var groupe = el('g', {}, g.svg);
      colonnesSvg.push(groupe);
      var base = 0;
      var visibles = series.filter(function (s) { return m[s.cle] > 0; });
      visibles.forEach(function (s, k) {
        var haut = y(base + m[s.cle]);
        var bas = y(base);
        // 2 px de vide sous chaque segment posé sur un autre
        var h = Math.max(0, bas - haut - (k > 0 ? 2 : 0));
        var x = xDe(i) - largeur / 2;
        var dessus = k === visibles.length - 1;
        var r = dessus ? Math.min(4, h) : 0;
        var d = 'M' + x + ' ' + (haut + h) + ' V' + (haut + r)
          + (r ? ' Q' + x + ' ' + haut + ' ' + (x + r) + ' ' + haut : '')
          + ' H' + (x + largeur - r)
          + (r ? ' Q' + (x + largeur) + ' ' + haut + ' ' + (x + largeur) + ' ' + (haut + r) : '')
          + ' V' + (haut + h) + ' Z';
        el('path', { d: d, style: 'fill: var(' + s.couleur + ')' }, groupe);
        base += m[s.cle];
      });
    });

    // Total au-dessus de la plus haute colonne seulement
    var iMax = totaux.indexOf(Math.max.apply(null, totaux));
    if (totaux[iMax] > 0) {
      var t = el('text', { x: xDe(iMax), y: y(totaux[iMax]) - 6, 'text-anchor': 'middle',
        style: 'fill: var(--kp-text); font-weight: 600' }, g.svg);
      t.textContent = format.format(totaux[iMax]);
    }

    zonesSurvol(g, xDe, bande, function (i) {
      colonnesSvg.forEach(function (c, k) { c.style.opacity = k === i ? '1' : '0.45'; });
      var lignes = series.map(function (s) { return { nom: s.nom, couleur: s.couleur, valeur: MOIS[i][s.cle] }; });
      afficherBulle(g, MOIS[i].libelleLong + ' · ' + format.format(totaux[i]), lignes, xDe(i));
    }, function () {
      colonnesSvg.forEach(function (c) { c.style.opacity = '1'; });
      masquerBulle(g);
    });
  }

  function dessiner() {
    var a = document.getElementById('graphique-activite');
    var d = document.getElementById('graphique-documents');
    if (a && a.clientWidth) courbes(a);
    if (d && d.clientWidth) colonnes(d);
  }

  var minuteur;
  window.addEventListener('resize', function () {
    window.clearTimeout(minuteur);
    minuteur = window.setTimeout(dessiner, 120);
  });
  dessiner();
})();
