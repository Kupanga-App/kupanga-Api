-- S2 (Sprint 5) : la base ne garde plus l'URL complète d'une photo, seulement sa clé dans le stockage S3.
-- L'URL est construite à la lecture (photos des biens : URL publique ; photo de profil : URL présignée de 24 h),
-- pour qu'un changement de domaine ou de serveur de stockage (sortie de MinIO) ne casse aucune photo.
--
-- La clé est extraite du dernier segment après /<bucket>/, quel que soit l'hôte (minio-backend.duckdns.org,
-- localhost:9000…). Une URL qui ne suit pas ce format (inattendue) est laissée telle quelle : la clé reste vide
-- et l'application continue d'afficher l'ancienne URL.
--
-- Transition (décision du 2026-11-02) : les anciennes colonnes d'URL des photos de biens sont gardées avec leurs
-- valeurs ; elles seront supprimées par une migration ultérieure, après la bascule vers le serveur de production
-- (Sprint 6). Les nouvelles photos n'y écrivent plus rien. Retour à la version précédente de l'application : les
-- photos de biens antérieures à V51 restent affichées, mais pas celles ajoutées depuis ni les photos de profil.

-- ─── Photos des biens (bucket public) ────────────────────────────────────────
ALTER TABLE bien_images ADD COLUMN cle VARCHAR(255);
ALTER TABLE bien_images ADD COLUMN cle_vignette VARCHAR(255);
ALTER TABLE bien_images ALTER COLUMN url DROP NOT NULL;

UPDATE bien_images
SET cle = substring(url FROM '/bucket-photo-immobilier/([A-Za-z0-9][A-Za-z0-9._-]*)$')
WHERE url IS NOT NULL;

UPDATE bien_images
SET cle_vignette = substring(url_vignette FROM '/bucket-photo-immobilier/([A-Za-z0-9][A-Za-z0-9._-]*)$')
WHERE url_vignette IS NOT NULL;

-- Une photo a toujours une clé ou une ancienne URL
ALTER TABLE bien_images ADD CONSTRAINT ck_bien_images_cle_ou_url CHECK (cle IS NOT NULL OR url IS NOT NULL);

-- ─── Photos de profil (bucket désormais privé) ───────────────────────────────
-- url_photo_profil ne garde que les photos externes (compte Google) ; nos photos passent en clé.
-- L'ancienne URL n'est pas gardée : le bucket devient privé, elle ne s'afficherait plus.
ALTER TABLE utilisateurs ADD COLUMN cle_photo_profil VARCHAR(255);

UPDATE utilisateurs
SET cle_photo_profil = substring(url_photo_profil FROM '/bucket-photo-profil/([A-Za-z0-9][A-Za-z0-9._-]*)$')
WHERE url_photo_profil IS NOT NULL;

-- Ne restent que les photos de comptes Google ; une autre URL (adresse externe saisie avant B5, ancien nom de
-- fichier non reconnu sur l'ancien hôte, désormais privé) ne s'afficherait pas ou servirait de pixel de suivi
UPDATE utilisateurs
SET url_photo_profil = NULL
WHERE cle_photo_profil IS NOT NULL
   OR (url_photo_profil IS NOT NULL
       AND url_photo_profil !~ '^https://[A-Za-z0-9.-]+\.googleusercontent\.com/');
