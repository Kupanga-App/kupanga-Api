-- ============================================================
-- V25 — Ajout signature propriétaire sur quittances
-- ============================================================

ALTER TABLE quittances
    ADD COLUMN IF NOT EXISTS signature_proprietaire      TEXT,
    ADD COLUMN IF NOT EXISTS date_signature_proprietaire TIMESTAMP;