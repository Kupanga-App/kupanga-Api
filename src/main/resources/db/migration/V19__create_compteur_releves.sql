-- ============================================================
-- V19 — Création de la table compteur_releves
--       Relevé de compteur rattaché à un état des lieux
-- ============================================================

CREATE TABLE compteur_releves (
    id                  BIGSERIAL       PRIMARY KEY,
    type_compteur       VARCHAR(50)     NOT NULL,   -- enum TypeCompteur
    numero_compteur     VARCHAR(100),
    index   DOUBLE PRECISION  NOT NULL,
    unite               VARCHAR(10),                -- "m³", "kWh"
    etat_des_lieux_id   BIGINT          NOT NULL
        REFERENCES etats_des_lieux(id) ON DELETE CASCADE
);

CREATE INDEX idx_compteurs_etat ON compteur_releves(etat_des_lieux_id);
