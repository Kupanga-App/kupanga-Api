-- V14__alter_table_contrats.sql

-- Nouvelles colonnes
ALTER TABLE contrats
    ADD COLUMN IF NOT EXISTS duree_bail_mois              INTEGER,
    ADD COLUMN IF NOT EXISTS charges_mensuelles           DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS depot_garantie               DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS adresse_bien                 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signature_proprietaire       TEXT,
    ADD COLUMN IF NOT EXISTS signature_locataire          TEXT,
    ADD COLUMN IF NOT EXISTS date_signature_proprietaire  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS date_signature_locataire     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS token_signature              VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS token_expiration             TIMESTAMP,
    ADD COLUMN IF NOT EXISTS statut                       VARCHAR(50) NOT NULL DEFAULT 'BROUILLON',
    ADD COLUMN IF NOT EXISTS created_at                   TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at                   TIMESTAMP DEFAULT NOW();

-- Contrainte sur le statut
ALTER TABLE contrats
    ADD CONSTRAINT chk_statut_contrat
    CHECK (statut IN (
        'BROUILLON',
        'EN_ATTENTE_SIGNATURE_PROPRIO',
        'EN_ATTENTE_SIGNATURE_LOCATAIRE',
        'SIGNE',
        'EXPIRE',
        'ANNULE'
    ));

-- Index
CREATE INDEX IF NOT EXISTS idx_contrats_bien_id        ON contrats (bien_id);
CREATE INDEX IF NOT EXISTS idx_contrats_proprietaire   ON contrats (proprietaire_id);
CREATE INDEX IF NOT EXISTS idx_contrats_locataire      ON contrats (locataire_id);
CREATE INDEX IF NOT EXISTS idx_contrats_statut         ON contrats (statut);
CREATE INDEX IF NOT EXISTS idx_contrats_token          ON contrats (token_signature);