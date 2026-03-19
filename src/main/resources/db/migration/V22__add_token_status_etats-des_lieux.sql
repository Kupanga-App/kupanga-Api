-- ============================================================
-- V22 — Ajout token de signature et statut sur etats_des_lieux
-- ============================================================

ALTER TABLE etats_des_lieux
    ADD COLUMN IF NOT EXISTS token_signature  VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS token_expiration TIMESTAMP,
    ADD COLUMN IF NOT EXISTS statut           VARCHAR(50) NOT NULL DEFAULT 'BROUILLON';