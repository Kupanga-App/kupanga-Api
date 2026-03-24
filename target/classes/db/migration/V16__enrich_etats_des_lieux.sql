-- ============================================================
-- V16 — Enrichissement de la table etats_des_lieux
--       Ajout : heure, signatures, audit
-- ============================================================

ALTER TABLE etats_des_lieux
    ADD COLUMN IF NOT EXISTS heure_realisation        TIME,
    ADD COLUMN IF NOT EXISTS signature_proprietaire   TEXT,
    ADD COLUMN IF NOT EXISTS date_signature_proprietaire TIMESTAMP,
    ADD COLUMN IF NOT EXISTS signature_locataire      TEXT,
    ADD COLUMN IF NOT EXISTS date_signature_locataire TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_at               TIMESTAMP DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at               TIMESTAMP DEFAULT now();

-- S'assurer que les colonnes existantes sont bien NOT NULL
ALTER TABLE etats_des_lieux
    ALTER COLUMN type              SET NOT NULL,
    ALTER COLUMN date_realisation  SET NOT NULL,
    ALTER COLUMN bien_id           SET NOT NULL,
    ALTER COLUMN proprietaire_id   SET NOT NULL,
    ALTER COLUMN locataire_id      SET NOT NULL;
