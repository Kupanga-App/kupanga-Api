-- ============================================================
-- V24 — Correction type colonne mois dans quittances
--       VARCHAR(20) → INTEGER
-- ============================================================

ALTER TABLE quittances
    ALTER COLUMN mois TYPE INTEGER USING mois::INTEGER;