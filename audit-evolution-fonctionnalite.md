# Audit des évolutions fonctionnelles — vers un vrai SaaS de gestion locative

> Rédigé le 2026-11-05, après les lots 1 à 3 et la fin de bail. Point de départ : l'application telle qu'elle est
> aujourd'hui (API Spring Boot + front Angular PWA, France et RDC). Objectif : qu'un particulier ou une agence
> **quitte Leboncoin, PAP ou SeLoger pour gérer ses logements ici**, et que des sociétés de gestion immobilière
> aient envie d'essayer.
>
> Ordre de priorité : **d'abord ce qui ne demande que du développement et ne coûte rien à faire tourner**
> (aucun service payant, aucune licence), puis ce qui dépend de services extérieurs.

---

## 1. Où en est Kupanga aujourd'hui

### Ce que l'application sait déjà faire

| Étape de la vie d'une location | Kupanga |
|---|---|
| Annonce | Publication avec photos réduites et sans EXIF, carte avec position arrondie, recherche avec filtres, PWA hors ligne |
| Premier contact | Messagerie temps réel liée au bien, premiers contacts plafonnés contre le spam |
| Choix du locataire | Demande de location (72 h) → réservation (14 j) → locataire à la signature du bail |
| Bail | Modèle par pays (France, RDC provisoire), signature en ligne par les deux parties, PDF figé |
| Entrée | État des lieux pièce par pièce, compteurs par pays (Cash Power, groupe électrogène…), signé en ligne |
| Vie du bail | Quittances PDF, déclaration du loyer reçu (espèces, Mobile Money, virement…), photo du reçu, rappels WhatsApp |
| Sortie | Congé (propriétaire ou locataire), fin du bail avec dépôt restitué et motif de retenue |
| Comptes | Un même compte peut posséder des biens et louer ailleurs ; droits par relation au bien |
| Pays | France et RDC dans le même code (devises, champs d'adresse, modèles, canaux de notification) |

### Ce qui nous distingue déjà des sites d'annonces

Leboncoin, PAP et SeLoger **s'arrêtent au premier contact** : une fois le locataire trouvé, tout se passe hors du
site (PDF envoyés par e-mail, signature papier, quittances faites à la main). Kupanga suit **toute la location**,
de l'annonce à la restitution du dépôt, avec un seul compte et un historique complet.

Trois atouts que personne n'a ensemble :

1. **Le parcours complet** annonce → bail → état des lieux → quittances → fin de bail, sans changer d'outil.
2. **L'Afrique centrale en plus de la France** : loyers en USD/CDF, adresses par commune et quartier, compteurs
   Cash Power, rappels WhatsApp, PWA sur réseau lent. Les logiciels de gestion français ignorent ce marché ; les
   sites d'annonces congolais ne gèrent pas la location.
3. **Aucun argent ne transite par l'application** : pas d'agrément de paiement, pas de commission, confiance plus
   facile à obtenir (décision du 2026-10-07).

### Ce qui manque pour qu'on quitte les autres

- **Côté candidat** : pas de dossier locataire, pas de favoris, pas d'alertes, pas de prise de rendez-vous de visite.
- **Côté propriétaire** : pas de vue financière, pas de révision de loyer, pas de relances automatiques, pas de
  suivi des travaux ni des échéances (diagnostics, assurance).
- **Côté entreprise** : pas d'équipe, pas de mandat de gestion, pas d'import de portefeuille, pas de reporting.
- **Côté confiance** : pas d'avis, pas de badges de vérification, pas de journal d'audit des signatures.

---

## 2. Concurrence : à qui on prend des utilisateurs

| | Sites d'annonces (Leboncoin, PAP, SeLoger) | Logiciels de gestion locative pour particuliers | **Kupanga (cible)** |
|---|---|---|---|
| Trouver un locataire | ✅ très fort (audience) | ❌ ou par diffusion payante | ✅ annonces + candidatures structurées |
| Dossier et choix du candidat | ⚠️ messages libres | ⚠️ partiel | ✅ dossier, comparaison, réservation |
| Bail signé en ligne | ⚠️ souvent payant ou absent | ✅ | ✅ inclus |
| État des lieux | ❌ | ✅ | ✅ + comparaison entrée/sortie |
| Quittances, loyers | ❌ | ✅ | ✅ + Mobile Money déclaré, WhatsApp |
| Afrique centrale | ❌ | ❌ | ✅ |
| Équipes / agences | ❌ | ⚠️ offres « pro » | ✅ cible des vagues 3-4 |
| Le locataire a son espace | ❌ | ⚠️ parfois | ✅ dès aujourd'hui |

**Conséquence** : on ne battra pas Leboncoin sur l'audience. On gagne en étant **le meilleur endroit où gérer la
location une fois l'annonce publiée** — et en rendant l'annonce Kupanga facile à partager partout (lien, QR code,
WhatsApp) pour que l'audience des autres sites ramène des candidats chez nous.

---

## 3. Vague 1 — Développement pur, gratuit (à faire en premier)

Effort : **S** = quelques jours, **M** = une à deux semaines, **L** = plus. Tout tient dans la stack actuelle
(Spring Boot, PostgreSQL, stockage S3 auto-hébergé, SMTP, Angular). Aucun abonnement.

### 3.1 Candidats et annonces (faire venir les gens)

| # | Fonctionnalité | Pourquoi ça fait la différence | Effort |
|---|---|---|---|
| F1 | **Dossier locataire numérique** : pièces (identité, justificatifs de revenus, garant) dans un bucket privé, partagées **seulement** avec les propriétaires auxquels le candidat postule, retirables à tout moment ; liste des pièces par pays (France : pièces autorisées par la loi) | Fin des dossiers envoyés en PDF par e-mail à des inconnus (risque d'usurpation) ; argument fort face à Leboncoin | M |
| F2 | **Candidature structurée** depuis l'annonce (bouton « Postuler » : message + dossier + date d'entrée souhaitée) et **tableau de comparaison** des candidats côté propriétaire | Remplace le tri des messages ; mène directement à la demande de location existante | M |
| F3 | **Visites** : le propriétaire propose des créneaux, le candidat en réserve un, rappels par e-mail, fichier `.ics` pour l'agenda | Gros temps perdu aujourd'hui en allers-retours de messages | M |
| F4 | **Favoris et alertes de recherche** (e-mail quand une annonce correspond) | Fait revenir les candidats ; indispensable face aux sites d'annonces | S |
| F5 | **Partage d'annonce** : lien court, QR code à imprimer (vitrine, panneau « À louer »), texte prêt pour WhatsApp et Facebook, page publique optimisée pour Google (balises, aperçu) | Récupère l'audience des autres sites et de la rue, surtout à Kinshasa | S |
| F6 | **Annonce plus riche** : plan des pièces, disponibilités, charges détaillées, frais d'agence à zéro mis en avant, « répond en moyenne en X h » | Confiance et qualité des candidatures | S |

### 3.2 Gestion du bien au quotidien (garder les propriétaires)

| # | Fonctionnalité | Pourquoi | Effort |
|---|---|---|---|
| F7 | **Quittances et appels de loyer automatiques** : génération mensuelle planifiée, rappel au locataire avant l'échéance, relances d'impayés graduées (aimable, ferme, mise en demeure à faire valider par un juriste) | Le propriétaire n'a plus rien à penser chaque mois | M |
| F8 | **Révision annuelle du loyer (France, IRL)** : calcul à la date anniversaire, courrier au locataire, nouvel indice saisi depuis la publication publique de l'INSEE (saisie manuelle par l'administrateur, pas d'API payante) | Fonction attendue par tout propriétaire français ; déjà au plan (« Ensuite ») | S |
| F9 | **Régularisation annuelle des charges** : provisions versées / charges réelles, décompte PDF | Obligation en France, souvent mal faite | M |
| F10 | **Tableau de bord financier** : revenus encaissés, impayés, taux d'occupation, rendement brut par bien ; export CSV et PDF annuel (aide à la déclaration des revenus fonciers, sans conseil fiscal) | La vue que les sites d'annonces n'ont pas | M |
| F11 | **Incidents et travaux** : le locataire signale (photos, urgence), le propriétaire suit (statut, intervenant, coût), historique par bien | Le motif n° 1 d'échanges pendant un bail | M |
| F12 | **Documents et échéances du bien** : diagnostics (DPE…), assurance habitation du locataire, attestations, contrats d'entretien, avec rappels avant expiration | Évite les oublis coûteux ; renforce le « dossier du logement » | S |
| F13 | **Calendrier unique des échéances** (loyers, révisions, fins de bail, diagnostics, visites) | Une seule vue « ce qui arrive » | S |
| F14 | **État des lieux de sortie comparé à l'entrée** : écarts pièce par pièce mis en évidence, photos par élément, proposition de retenue justifiée reliée à la fin de bail | Différenciant fort, réduit les litiges sur le dépôt | M |
| F15 | **Modèles de courriers** (congé, mise en demeure, attestation de loyer, relance) générés en PDF depuis les données du bail, par pays | Gain de temps ; texte à faire valider par un juriste | S |
| F16 | **Colocation** : plusieurs locataires sur une location, quote-parts du loyer, bail commun ou individuel | Marché important (étudiants, jeunes actifs) ; déjà prévu par le modèle `Location` | L |

### 3.3 Confiance et sécurité (convaincre de laisser les autres)

| # | Fonctionnalité | Pourquoi | Effort |
|---|---|---|---|
| F17 | **Journal d'audit des documents** : qui a créé, vu, signé, quand, depuis quelle IP ; inclus en annexe du PDF signé ; **code de signature à usage unique envoyé par e-mail** | Renforce la valeur probante de la signature sans prestataire payant (déjà au plan) | M |
| F18 | **Badges de confiance** : e-mail vérifié (existe), téléphone renseigné, « a déjà signé un bail sur Kupanga », ancienneté du compte | Lutte contre les fausses annonces, plaie des sites gratuits | S |
| F19 | **Avis après location** (propriétaire ↔ locataire, seulement entre parties d'un bail terminé, droit de réponse, modération par le back-office) | Réputation impossible à obtenir sur Leboncoin | M |
| F20 | **Signalement d'annonce frauduleuse** + file de modération dans le back-office | Indispensable avant l'ouverture au grand public | S |
| F21 | **RGPD** : export de toutes ses données, suppression du compte en libre-service, CGU, politique de confidentialité, registre des traitements | Bloquant de mise en ligne (déjà au plan) | M |

### 3.4 Préparer les entreprises (sans rien payer)

| # | Fonctionnalité | Pourquoi | Effort |
|---|---|---|---|
| F22 | **Gestionnaire mandaté (C15)** : le propriétaire délègue un bien à un gestionnaire (famille, agent, diaspora) avec des droits choisis (annonce, bail, quittances…) ; le propriétaire voit tout | Besoin réel en RDC (propriétaires à l'étranger) et première marche vers les agences | M |
| F23 | **Organisation** : une agence crée son espace, invite ses collaborateurs, répartit les biens, droits par membre | Ce qui fait passer d'un outil de particulier à un SaaS | L |
| F24 | **Import et export du portefeuille** (CSV : biens, locataires, baux en cours) | Personne ne migre s'il faut tout ressaisir | M |
| F25 | **Rapport au propriétaire mandant** (mensuel, PDF) : loyers encaissés, incidents, échéances | Ce que les agences envoient aujourd'hui à la main | M |
| F26 | **Langues** : anglais en plus du français (diaspora, expatriés) ; interface prête pour d'autres langues | Ouvre la RDC anglophone d'affaires et la diaspora | M |

---

## 4. Vague 2 — Dépend de services extérieurs (plus tard, quand il y aura des revenus)

| # | Fonctionnalité | Dépendance | Remarque |
|---|---|---|---|
| P1 | Paiement du loyer en ligne (carte, SEPA, Mobile Money) | Prestataire de paiement ou agrégateur Mobile Money | Revient sur la décision « aucun paiement via l'app » : à rediscuter, gros impact juridique |
| P2 | Signature électronique qualifiée (eIDAS) | Prestataire de confiance (payant à la signature) | Option « premium » pour les agences ; la signature actuelle reste gratuite |
| P3 | Vérification d'identité (KYC) | Prestataire de vérification | Badge « identité vérifiée » |
| P4 | SMS et WhatsApp automatiques | Opérateur SMS, WhatsApp Business API | Aujourd'hui : liens `wa.me` gratuits envoyés à la main |
| P5 | Diffusion des annonces sur les portails | Accords ou flux payants des portails | Utilise leur audience sans les remplacer |
| P6 | Garantie loyers impayés, assurance habitation | Partenaires assureurs | Source de revenus par commission |
| P7 | Rapprochement bancaire automatique des loyers | Agrégateur bancaire (France) | Supprime la déclaration manuelle du loyer reçu |
| P8 | Visite virtuelle 360°, estimation du loyer | Hébergement vidéo, données de marché | Confort, pas prioritaire |

---

## 5. Vision long terme : ce qu'on montre à une entreprise

**Le message** : « Un seul outil, de l'annonce à la restitution du dépôt, pour vous, vos propriétaires et vos
locataires, en France comme à Kinshasa — sans payer pour chaque bail, chaque signature, chaque quittance. »

Ce qui doit être visible dès la démo :

1. **Le parcours complet en 5 minutes** : publier → recevoir une candidature avec dossier → réserver → signer le
   bail → état des lieux → première quittance. Aucun autre outil gratuit ne le fait.
2. **Le portail locataire inclus** : le locataire a son espace (documents, quittances, incidents, congé). Moins
   d'appels, moins d'e-mails pour l'agence.
3. **Le tableau de bord du portefeuille** : occupation, impayés, échéances, travaux, sur tous les biens.
4. **Le multi-pays** : un gestionnaire basé à Paris gère des biens à Kinshasa avec les bonnes devises, les bons
   compteurs, les bons canaux.

**Modèle économique possible** (à décider plus tard) : gratuit pour un particulier jusqu'à N biens ; abonnement
par bien ou par organisation au-delà (équipes, rapports, import, signature qualifiée, marque blanche) ;
commissions sur les services partenaires (vague 2). Le cœur de gestion reste gratuit : c'est l'argument contre
les sites d'annonces et contre les logiciels payants.

---

## 6. Feuille de route proposée

| Étape | Contenu | Résultat |
|---|---|---|
| **Avant l'ouverture** | F21 (RGPD), F20 (signalement), F17 (journal d'audit), recette des lots 1 à 3 et de la fin de bail | Mise en ligne possible |
| **Vague 1-a « candidats »** | F1, F2, F4, F5 | Les candidats préfèrent postuler ici |
| **Vague 1-b « propriétaires »** | F7, F8, F10, F11, F14 | Les propriétaires n'ont plus besoin d'autre outil |
| **Vague 1-c « confiance »** | F3, F12, F13, F15, F18, F19 | Kupanga devient la référence fiable |
| **Vague 1-d « pro »** | F22, F24, F25, puis F23, F26, F16 | Les agences peuvent s'y installer |
| **Vague 2** | P1 à P8 selon les revenus et les partenaires | Services payants, revenus |

---

## 7. Points d'attention

- **Juridique** : liste des pièces exigibles d'un candidat (France), modèles de courriers, préavis et restitution
  du dépôt, avis publics (diffamation, droit de réponse), modèles RDC `cd-v1` : **validation par un juriste avant
  chaque mise en ligne** (comme pour J5).
- **Données sensibles** : le dossier locataire (F1) et les avis (F19) demandent chiffrement au repos des pièces,
  durées de conservation, accès journalisés, inscription au registre RGPD.
- **Décisions déjà prises à respecter** : aucun paiement via l'application (revue seulement en vague 2), pas d'OTP
  SMS (le code de signature passe par e-mail), monolithe Spring Boot, stockage S3 auto-hébergé.
- **Charge serveur** : alertes de recherche (F4) et quittances automatiques (F7) ajoutent des tâches planifiées et
  des e-mails : surveiller la file d'e-mails et le quota SMTP.
