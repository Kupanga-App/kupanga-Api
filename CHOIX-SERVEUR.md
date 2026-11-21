# Hébergement du pilote : choix et budget

> **Date** : 21 novembre 2026 (remplace la version du 2 novembre, pensée pour une production avec utilisateurs).
> **Contexte** : lancement **en pilote**, sans utilisateurs au départ et sans certitude qu'il y en aura. Le but est
> donc de dépenser le moins possible tout en ayant une application **complète, en HTTPS, sur son propre domaine,
> avec des e-mails qui arrivent**, et de pouvoir grandir sans tout refaire si le pilote prend.
> **Prix** : relevés en novembre 2026 sur les sites des fournisseurs et des comparateurs (liens en fin de document).
> Ils bougent souvent (hausses chez OVHcloud et Hetzner en 2026) : **les revérifier dans le panier le jour de la
> commande**. Montants **TTC** (TVA française 20 %), pour un particulier qui ne récupère pas la TVA.

---

## 1. La recommandation en une page

| Poste | Choix | Coût TTC |
|---|---|---|
| Serveur (front, API, base, Redis, fichiers S3, HTTPS) | **VPS-1 OVHcloud** : 2 vCores, 4 Go, 40 Go NVMe, sauvegarde quotidienne incluse | **4,57 € / mois** |
| Nom de domaine `.com` | **OVHcloud** | **9,59 € la 1re année**, puis **≈ 17,15 € / an** |
| Boîte `contact@domaine` : recevoir, répondre **et** envoyer les e-mails de l'application | **Messagerie incluse avec le domaine OVHcloud**, utilisée aussi comme serveur SMTP de l'application (≈ 200 e-mails / heure) | **0 €** |
| Relais d'envoi de secours (si les e-mails arrivent en spam) | Brevo, offre gratuite (300 e-mails / jour) : seulement en cas de besoin | **0 €** |
| Sauvegarde hors du serveur (base + fichiers, chiffrée) | **Cloudflare R2**, offre gratuite (10 Go) | **0 €** |
| DNS, HTTPS, cache des fichiers statiques, protection | **Cloudflare** (offre gratuite) + Caddy (certificats Let's Encrypt) | **0 €** |
| Suivi des erreurs et disponibilité | Sentry (offre gratuite), UptimeRobot ou Better Stack (gratuit) | **0 €** |
| **Total** | | **≈ 5,40 € / mois la 1re année**, **≈ 6 € / mois** ensuite |

Soit **≈ 64 € la première année** et **≈ 72 € par an** ensuite : bien sous le budget visé de 10 € par mois.
La marge restante sert de réserve pour passer au VPS-2 dès que le pilote décolle (§ 5).

---

## 2. Le serveur : VPS-1 OVHcloud

**Pourquoi un seul VPS pour tout.** Les offres gratuites « en morceaux » (base chez Neon, Redis chez Upstash,
API chez Render…) ont déjà été essayées : démarrages lents, mises en veille, quotas, et la tentative Oracle Cloud
gratuite a échoué faute de capacité (novembre 2026). Un petit VPS payant regroupe tout, ne s'endort jamais et coûte
moins qu'un café par mois.

**Pourquoi OVHcloud.** Le prix le plus bas pour 4 Go de RAM, un hébergeur français (données en France, facture en
euros, support en français), une sauvegarde quotidienne incluse et le même compte pour le domaine et la boîte mail.

| Offre | vCores | RAM | Disque | Prix TTC / mois | Remarque |
|---|---|---|---|---|---|
| **OVHcloud VPS-1** | 2 | 4 Go | 40 Go NVMe | **4,57 €** | **choisi pour le pilote** |
| OVHcloud VPS-2 | 4 | 8 Go | 75 Go NVMe | 8,65 € | palier suivant (§ 5) |
| Hetzner CX23 | 2 | 4 Go | 40 Go | ≈ 7,20 € (5,49 € HT + 0,50 € IPv4 + TVA) | plus cher depuis la hausse de juin 2026, souvent en rupture |
| Hetzner CAX11 (ARM) | 2 | 4 Go | 40 Go | ≈ 7,80 € | ARM : images Docker à construire pour ARM |

### Ce qui tient dans 4 Go

Tout tourne en conteneurs Docker sur la même machine, derrière Caddy :

| Service | Mémoire visée sur le VPS-1 |
|---|---|
| API Spring Boot (tas Java 512 Mo) | ≈ 0,8 Go |
| PostgreSQL + PostGIS (réglages réduits) | ≈ 0,6 Go |
| RustFS (fichiers S3 : photos, PDF) | ≈ 0,3 Go |
| Redis + Caddy (qui sert aussi le front Angular, fichiers statiques) | ≈ 0,1 Go |
| Système + cache disque | ≈ 2 Go restants |

C'est suffisant pour un pilote : **une centaine d'utilisateurs actifs en même temps** et quelques milliers de
comptes (estimation, pas une mesure). Deux précautions :

- **construire les images Docker sur GitHub Actions**, pas sur le VPS (une compilation Maven + Angular sature 4 Go) ;
- garder les limites déjà en place dans l'application : 2 réencodages de photos à la fois, tas Java plafonné.

Variante : le front Angular peut aussi être servi gratuitement par **Cloudflare Pages**, ce qui soulage un peu le VPS.
Le servir par Caddy sur le VPS reste plus simple (un seul endroit à déployer) et suffit pour le pilote.

**Pour les utilisateurs au Congo** : le serveur est en France ; compter environ 150 à 250 ms de latence depuis
Kinshasa. Cloudflare (gratuit) met en cache près des utilisateurs le front, les photos et les icônes, ce qui couvre
l'essentiel du poids des pages.

---

## 3. Le nom de domaine et les e-mails

### Domaine `.com`

| Registraire | 1re année | Renouvellement / an | Boîte mail incluse |
|---|---|---|---|
| **OVHcloud** | ≈ 9,59 € TTC (7,99 € HT) | ≈ 17,15 € TTC (14,29 € HT) | **oui** (« messagerie Starter ») |
| Cloudflare Registrar | ≈ 11,17 $ (prix coûtant) | ≈ 11,17 $ | non (redirection seulement) |
| Porkbun | ≈ 11 à 12 $ | ≈ 11 à 12 $ | non |

**Choix : OVHcloud.** Cloudflare est quelques euros moins cher par an, mais sans boîte de réception : il faudrait
alors bricoler une redirection vers Gmail avec un envoi « en tant que », moins fiable (Gmail ne signe pas avec la clé
DKIM du domaine). Chez OVHcloud, la boîte `contact@` est incluse, et le domaine, le VPS et la facture sont au même
endroit. Le DNS du domaine est ensuite confié à **Cloudflare** (gratuit), pour le HTTPS, le cache et l'IP réelle des
visiteurs.

Le prix d'appel de la première année est promotionnel : le budget compte le prix de renouvellement dès la 2e année.

### Une seule boîte pour recevoir et pour envoyer

La boîte `contact@domaine` incluse avec le domaine sert à deux choses :

| Usage | Comment | Coût |
|---|---|---|
| **Échanges humains** : lire et répondre aux utilisateurs | webmail OVHcloud ou application mail (IMAP) | 0 € |
| **Envois de l'application** : confirmation d'inscription, mot de passe oublié, invitations à signer, quittances | l'application se connecte au serveur SMTP de la boîte (`smtp.mail.ovh.net`, port 465 en SSL) ; l'expéditeur est `contact@domaine` | 0 € |

L'application est déjà prête : elle envoie par n'importe quel serveur SMTP (configuration `SMTP_*` du Sprint 5) et,
en production, exige une connexion chiffrée, ce que le port 465 en SSL fournit.

```
SMTP_HOST=smtp.mail.ovh.net
SMTP_PORT=465
SMTP_SSL=true
SMTP_STARTTLS=false
SMTP_USERNAME=contact@domaine.com
SMTP_PASSWORD=<mot de passe de la boîte>
EMAIL_EXPEDITEUR=contact@domaine.com
```

**Limites** : environ **200 e-mails par heure** par boîte et 100 destinataires par e-mail (OVHcloud). C'est plus
large que l'offre gratuite de Brevo (300 par jour) et très au-delà des besoins d'un pilote ; en cas de dépassement,
la file d'envoi de l'application réessaie plus tard sans rien perdre.

**Pour que les e-mails n'arrivent pas en spam** (gratuit, une demi-heure) : dans le DNS Cloudflare, publier le
SPF d'OVHcloud, activer la signature **DKIM** de la boîte dans l'espace client OVHcloud et recopier ses
enregistrements, puis ajouter un **DMARC** (d'abord en surveillance, `p=none`). Contrôler ensuite avec un service
comme mail-tester.com.

**Ce qu'on accepte** : pas de tableau de bord des envois (rebonds, plaintes), et une réputation d'envoi partagée
avec d'autres clients d'OVHcloud. Une plainte pour spam pénaliserait aussi `contact@`, qui sert aux échanges humains.

**Solution de secours** : si des e-mails importants arrivent en spam, passer à **Brevo** (offre gratuite, relais
`smtp-relay.brevo.com:587`, domaine authentifié) ou à **Resend** (3 000 par mois, 100 par jour), en changeant
seulement les variables `SMTP_*` : aucune modification du code.

---

## 4. Sauvegardes : ne pas tout perdre avec le serveur

Un VPS unique reste un point de panne unique. Deux niveaux, gratuits pour le pilote :

1. **Sauvegarde quotidienne d'OVHcloud** (incluse dans le VPS-1) : remet le serveur entier à l'état de la veille.
2. **Sauvegarde chiffrée hors d'OVHcloud, sur Cloudflare R2** (compatible S3, 10 Go gratuits, sortie gratuite) :
   chaque nuit, un `pg_dump` de la base et une copie des fichiers RustFS (rclone avec chiffrement), procédure déjà
   écrite dans `docs/sauvegardes.md`. Un pilote occupe quelques centaines de Mo : on reste longtemps dans le gratuit
   (au-delà : 0,015 $ par Go et par mois).

Une restauration testée sur un VPS neuf, une fois, avant d'ouvrir le pilote : c'est elle qui prouve que les
sauvegardes servent.

---

## 5. Budget sur deux ans et paliers

| | 1re année | 2e année |
|---|---|---|
| VPS-1 OVHcloud (12 × 4,57 €) | 54,84 € | 54,84 € |
| Domaine `.com` OVHcloud (boîte `contact@` incluse) | 9,59 € | 17,15 € |
| E-mails (boîte OVHcloud), Cloudflare, R2, Sentry, surveillance | 0 € | 0 € |
| **Total TTC** | **≈ 64 €** (≈ 5,40 € / mois) | **≈ 72 €** (≈ 6 € / mois) |

**Quand dépenser plus** (seulement si le pilote prend) :

| Signal | Action | Nouveau coût |
|---|---|---|
| Mémoire ou CPU au-dessus de 70 % aux heures de pointe, ou plus d'une centaine d'utilisateurs actifs en même temps | passer au **VPS-2** (OVHcloud permet en général de monter en gamme sans réinstaller : à vérifier à la commande) | + 4,08 € / mois (≈ 10 € / mois au total) |
| Plus de 200 e-mails par heure, ou e-mails en spam | relais dédié : Brevo ou Resend (gratuits d'abord, puis payants) | 0 €, puis ≈ 9 à 20 € / mois selon le volume |
| Sauvegardes au-delà de 10 Go | R2 payant, ou Backblaze B2 (6,95 $ par To) | quelques centimes à quelques euros / mois |
| Plusieurs personnes doivent répondre aux e-mails | boîtes supplémentaires chez OVHcloud, ou Zoho Mail (gratuit jusqu'à 5 utilisateurs, sans IMAP) | 0 à quelques euros / mois |

Le budget de 10 € par mois couvre donc aussi le premier palier (VPS-2) sans rien changer d'autre.

---

## 6. Ce qui est volontairement écarté pour le pilote

- **Les offres « gratuites » en morceaux** (Render, Neon, Upstash, Oracle Always Free) : mises en veille, quotas,
  capacité non garantie ; déjà essayées.
- **Un serveur plus gros dès le départ** (VPS-2 et au-delà) : utile seulement avec de vrais utilisateurs ; on y
  passe en quelques minutes le moment venu.
- **Un stockage S3 payant** (S3 d'Amazon, R2 pour les fichiers en ligne) : RustFS sur le VPS suffit ; le code est
  déjà portable vers n'importe quel S3 (Sprint 5).
- **Google Workspace ou Microsoft 365** pour la boîte mail (≈ 6 à 7 € par mois et par utilisateur) : la boîte
  incluse avec le domaine suffit pour un `contact@`.

---

## 7. Ordre de mise en place

1. Commander le **domaine `.com`** et le **VPS-1** chez OVHcloud (Gravelines ou Strasbourg).
2. Créer un compte **Cloudflare** gratuit, y déplacer le DNS du domaine (enregistrements MX de la boîte OVHcloud
   compris).
3. Créer la boîte **`contact@domaine`** chez OVHcloud, activer sa signature DKIM et publier SPF, DKIM et DMARC
   dans Cloudflare ; vérifier avec mail-tester.com.
4. Garder Brevo ou Resend en réserve (rien à créer tant que la boîte OVHcloud suffit).
5. Créer le bucket **Cloudflare R2** des sauvegardes et une clé d'accès limitée à ce bucket.
6. Déployer (Sprint 6) : Docker Compose de production, Caddy, sauvegardes nocturnes, surveillance ; puis tester une
   restauration avant d'ouvrir le pilote.

---

## Sources (novembre 2026)

- OVHcloud, gamme VPS : <https://www.ovhcloud.com/fr/vps/> et comparatif <https://www.ovhcloud.com/fr/vps/compare/> ;
  annonce de la gamme VPS 2027 : <https://blog.ovhcloud.com/vps-2027/>
- Prix des domaines OVHcloud (`.com`, messagerie incluse) : <https://www.citycom-france.fr/ovh-nom-de-domaine-tarifs-et-options-en-2026/>,
  <https://domains-defender.com/blog/renouveler-nom-de-domaine-ovh>
- Comparatif des registraires (renouvellement compris) : <https://dev.to/neurau_tech/nom-de-domaine-pas-cher-en-novembre-2026-11-registrars-compares-renouvellement-compris-4i5c>,
  <https://costgoat.com/pricing/cloudflare-domains>, <https://porkbun.com/about/domain-cost-calculator/com>
- Hetzner (hausse de juin 2026, CX23, IPv4) : <https://www.eucloudcost.com/providers/hetzner/>, <https://trackvps.com/hetzner-cloud-pricing>
- OVHcloud, quotas d'envoi des boîtes mail (≈ 200 e-mails par heure) : <https://docs.ovhcloud.com/en/guides/web-cloud/web-hosting/email-sending-best-practices>,
  <https://queensmtp.com/smtp-settings/ovh>, <https://community.ovhcloud.com/t/limite-envoi-de-mail-types-de-comptes/13253>
- Brevo, offre gratuite et SMTP : <https://www.brevo.com/free-smtp-server/>, <https://www.emailtooltester.com/en/reviews/brevo/>
- Resend, offre gratuite : <https://freetier.co/directory/products/resend>
- Zoho Mail, offre gratuite : <https://www.zoho.com/mail/custom-domain-email.html>
- Cloudflare Email Routing (redirection seulement) : <https://developers.cloudflare.com/email-routing/get-started/enable-email-routing/>
- Cloudflare R2, offre gratuite : <https://nayankyada.com/blog/cloudflare-r2-pricing-2026-free-tier-limits-egress-costs-when-to-upgrade>
- Backblaze B2 (6,95 $ par To) : <https://upstash.com/blog/backblaze-b2-pricing-explained-2026>
