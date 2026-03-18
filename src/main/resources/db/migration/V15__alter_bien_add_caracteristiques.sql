-- V15__alter_bien_add_caracteristiques.sql

ALTER TABLE biens
    -- Caractéristiques physiques
    ADD COLUMN IF NOT EXISTS surface_habitable    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS nombre_pieces        INTEGER,
    ADD COLUMN IF NOT EXISTS nombre_chambres      INTEGER,
    ADD COLUMN IF NOT EXISTS etage               INTEGER,
    ADD COLUMN IF NOT EXISTS ascenseur           BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS annee_construction  INTEGER,
    ADD COLUMN IF NOT EXISTS mode_chauffage      VARCHAR(30),

    -- Diagnostic énergétique
    ADD COLUMN IF NOT EXISTS classe_energie      VARCHAR(2),
    ADD COLUMN IF NOT EXISTS classe_ges          VARCHAR(2),

    -- Conditions de location
    ADD COLUMN IF NOT EXISTS loyer_mensuel       DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS charges_mensuelles  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS depot_garantie      DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS meuble              BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS colocation          BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS disponible_de       DATE;

-- Contraintes
ALTER TABLE biens
    ADD CONSTRAINT chk_classe_energie
        CHECK (classe_energie IN ('A','B','C','D','E','F','G')),
    ADD CONSTRAINT chk_classe_ges
        CHECK (classe_ges IN ('A','B','C','D','E','F','G')),
    ADD CONSTRAINT chk_mode_chauffage
        CHECK (mode_chauffage IN (
            'ELECTRIQUE','GAZ','FIOUL','BOIS',
            'POMPE_A_CHALEUR','POELE','COLLECTIF','SANS_CHAUFFAGE'
        )),
    ADD CONSTRAINT chk_surface
        CHECK (surface_habitable > 0),
    ADD CONSTRAINT chk_loyer
        CHECK (loyer_mensuel >= 0),
    ADD CONSTRAINT chk_charges
        CHECK (charges_mensuelles >= 0),
    ADD CONSTRAINT chk_depot
        CHECK (depot_garantie >= 0);

-- Index utiles pour la recherche
CREATE INDEX IF NOT EXISTS idx_biens_loyer          ON biens (loyer_mensuel);
CREATE INDEX IF NOT EXISTS idx_biens_surface        ON biens (surface_habitable);
CREATE INDEX IF NOT EXISTS idx_biens_disponible     ON biens (disponible_de);
CREATE INDEX IF NOT EXISTS idx_biens_meuble         ON biens (meuble);
CREATE INDEX IF NOT EXISTS idx_biens_classe_energie ON biens (classe_energie);