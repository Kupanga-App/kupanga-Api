-- ============================================================
-- V24 — Correction type colonne mois dans quittances
--       INTEGER → VARCHAR(20)
-- ============================================================

ALTER TABLE quittances
    ALTER COLUMN mois TYPE VARCHAR(20)
    USING mois::VARCHAR;