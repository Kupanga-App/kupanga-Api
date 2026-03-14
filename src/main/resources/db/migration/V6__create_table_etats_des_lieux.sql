-- =====================================================
-- Migration table etats_des_lieux
-- Alignée avec l'entité EtatDesLieux.java
-- =====================================================

CREATE TABLE IF NOT EXISTS etats_des_lieux (
    id                BIGSERIAL PRIMARY KEY,
    type              VARCHAR(50),    -- ENTREE / SORTIE
    observations      TEXT,
    url_pdf           VARCHAR(500),
    date_realisation  DATE,
    bien_id           BIGINT,
    proprietaire_id   BIGINT,
    locataire_id      BIGINT,

    CONSTRAINT fk_edl_bien         FOREIGN KEY (bien_id)         REFERENCES biens(id),
    CONSTRAINT fk_edl_proprietaire FOREIGN KEY (proprietaire_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_edl_locataire    FOREIGN KEY (locataire_id)    REFERENCES utilisateurs(id)
);

-- Index sur FK
CREATE INDEX IF NOT EXISTS idx_edl_bien_id         ON etats_des_lieux(bien_id);
CREATE INDEX IF NOT EXISTS idx_edl_proprietaire_id ON etats_des_lieux(proprietaire_id);
CREATE INDEX IF NOT EXISTS idx_edl_locataire_id    ON etats_des_lieux(locataire_id);

-- Index sur type (filtre ENTREE/SORTIE)
CREATE INDEX IF NOT EXISTS idx_edl_type ON etats_des_lieux(type);

-- Index sur date (tri chronologique)
CREATE INDEX IF NOT EXISTS idx_edl_date_realisation ON etats_des_lieux(date_realisation);