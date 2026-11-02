# Choix du serveur de production — VPS OVHcloud

> **Date** : 2 novembre 2026
> **Source des prix** : grille OVHcloud « VPS 2027 » relevée le 2026-11-02 (capture `docs/cout_vps_OVH.png`, non
> versionnée). Les prix évoluent : les revérifier sur le site d'OVHcloud le jour de la commande.
> **Contexte** : tout le back-end tourne sur **une seule machine** (cf. [`AUDIT-INFRA.md`](AUDIT-INFRA.md) §3,
> Sprint 6), sur une infrastructure et une base neuves.

---

## 1. Recommandation

**VPS-2 : 4 vCores, 8 Go de RAM, 75 Go NVMe, 1 Gbit/s — 8,65 € TTC / mois.**

C'est le meilleur rapport prix / tranquillité pour faire tourner tout le back-end sur un seul serveur. Le VPS-1
suffirait pour un pilote, mais sans marge : une rafale d'envois de photos ou de générations de PDF peut le saturer
en mémoire et le faire planter.

---

## 2. Les offres

| | VPS-1 | VPS-2 | VPS-3 | VPS-4 |
|---|---|---|---|---|
| vCores | 2 | 4 | 6 | 8 |
| RAM | 4 Go | 8 Go | 12 Go | 24 Go |
| Disque NVMe | 40 Go | 75 Go | 100 Go | 200 Go |
| Bande passante publique | 500 Mbit/s | 1 Gbit/s | 2 Gbit/s | 3 Gbit/s |
| Trafic | illimité | illimité | illimité | illimité |
| Sauvegarde OVH incluse | quotidienne (24 h) | quotidienne (24 h) | quotidienne (24 h) | quotidienne (24 h) |
| Prix HT / mois | 3,81 € | 7,21 € | 10,40 € | 19,96 € |
| **Prix TTC / mois** | **4,57 €** | **8,65 €** | **12,48 €** | **23,95 €** |

---

## 3. Ce que le back-end consomme

Six services tournent sur la même machine : l'API Spring Boot, PostgreSQL + PostGIS, Redis, RustFS (fichiers),
Caddy (HTTPS) et le système.

### Répartition de la mémoire visée

| Service | VPS-1 (4 Go) | VPS-2 (8 Go) | VPS-3 (12 Go) | VPS-4 (24 Go) |
|---|---|---|---|---|
| API (Java) | 1,2 Go (tas 768 Mo) | 2 Go (tas 1,25 Go) | 3 Go | 5 Go |
| PostgreSQL + PostGIS | 0,8 Go | 1,5 Go | 3 Go | 6 Go |
| RustFS + Redis + Caddy | 0,7 Go | 0,9 Go | 1,2 Go | 1,5 Go |
| Système + cache disque | ~1,3 Go (**juste**) | ~3,6 Go | ~4,8 Go | ~11,5 Go |

### Ce qui coûte du processeur

| Opération | Coût approximatif | Remarque |
|---|---|---|
| Connexion ou inscription (hachage BCrypt du mot de passe) | 50 à 100 ms de CPU | voulu : freine les attaques par force brute |
| Photo réencodée en 1600 px + vignette | 0,3 à 1 s de CPU | 2 au plus en même temps (limite du Sprint 4) |
| PDF de bail, d'EDL ou de quittance | 0,2 à 0,5 s | à la signature ou à la création |
| Lecture simple de l'API (biens, tableau de bord…) | quelques ms | l'essentiel du trafic |
| Photos et PDF téléchargés | ~0 pour l'API | servis par RustFS, mis en cache par Cloudflare |

---

## 4. Capacités estimées

> Ce sont des **estimations d'ordre de grandeur**, pas des mesures. Hypothèses : usage typique d'une
> application de gestion locative (un utilisateur actif fait une requête toutes les 5 à 20 secondes), cœurs
> partagés des VPS OVH, Cloudflare devant le serveur. **Seul un test de charge sur le vrai serveur peut les
> confirmer** : prévu au Sprint 6 (k6 ou Gatling).

| | VPS-1 | **VPS-2** | VPS-3 | VPS-4 |
|---|---|---|---|---|
| Prix TTC / mois | 4,57 € | **8,65 €** | 12,48 € | 23,95 € |
| Requêtes simples par seconde (pic) | 100 à 200 | **300 à 500** | 500 à 800 | 800 à 1 200 |
| Connexions ou inscriptions par seconde | 15 à 20 | **35 à 45** | 50 à 70 | 70 à 90 |
| Utilisateurs actifs en même temps | 100 à 200 | **500 à 1 000** | 1 000 à 1 500 | 2 000 à 3 000 |
| Connexions WebSocket ouvertes (chat) | 300 à 500 | **2 000 à 3 000** | 4 000 à 5 000 | 8 000 à 10 000 |
| Comptes inscrits (≈ 5 à 10 % actifs aux heures de pointe) | 2 000 à 5 000 | **10 000 à 25 000** | 30 000 à 50 000 | 50 000 à 100 000 |
| Photos stockables (disque, après système et base) | ~60 000 | **~150 000** (~15 000 biens) | ~200 000 | ~450 000 |
| Bande passante | 500 Mbit/s | 1 Gbit/s | 2 Gbit/s | 3 Gbit/s |
| **Verdict** | pilote seulement | **lancement et croissance** | grosse croissance | trop pour démarrer |

---

## 5. Ce qui limitera avant le serveur

1. **Le quota du fournisseur d'e-mails.** L'offre gratuite de Brevo, par exemple, est de **300 e-mails par jour**,
   soit environ 300 inscriptions par jour au maximum (chacune envoie un lien de confirmation). La file d'envoi ne
   perd rien, mais les e-mails en trop attendent. Passer à une offre payante ou à un autre relais SMTP se fait par
   configuration (`SMTP_*`).
2. **Nominatim** (géocodage des adresses, OpenStreetMap) : sa politique d'usage impose **1 requête par seconde**
   au plus, ce qui limite les créations de biens quel que soit le VPS.
3. **Le chat temps réel** tourne en mémoire, sur une seule instance. Il tient très bien sur un VPS, mais passer à
   deux serveurs demandera RabbitMQ (décision M3 du Sprint 5).

---

## 6. « Sans tomber en panne »

Aucun de ces VPS ne protège à lui seul contre une panne : **un serveur unique reste un point de défaillance
unique**. Ce qui protège réellement :

- la sauvegarde quotidienne fournie par OVH (dernières 24 h), **plus** une sauvegarde chiffrée chez un autre
  fournisseur (Sprint 6, H4) ;
- le redémarrage automatique des conteneurs et une surveillance avec alertes (Sprint 6, H6) ;
- une procédure de restauration testée chaque mois, avec un objectif de service rétabli en moins de 4 h sur un VPS
  neuf.

---

## 7. En pratique

1. Démarrer sur le **VPS-2**.
2. Surveiller la mémoire et le CPU pendant les premiers mois (Sprint 6, H6).
3. Passer au **VPS-3** quand la RAM ou le CPU restent au-dessus de 70 % aux heures de pointe, soit en gros à partir
   de quelques milliers d'utilisateurs actifs par jour. OVH permet en général de monter en gamme sans tout
   réinstaller (à vérifier au moment de la commande).
4. Au-delà, plutôt que le VPS-4 : séparer la base sur sa propre machine, ou passer à deux instances de l'API avec
   RabbitMQ (cf. [`AUDIT-INFRA.md`](AUDIT-INFRA.md) §2.3).
