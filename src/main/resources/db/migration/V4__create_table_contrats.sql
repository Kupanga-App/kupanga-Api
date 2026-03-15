-- =====================================================
-- Migration table contrats
-- Alignée avec l'entité Contrat.java
-- =====================================================

CREATE TABLE IF NOT EXISTS contrats (
    id              BIGSERIAL PRIMARY KEY,
    date_debut      DATE,
    date_fin        DATE,
    loyer_mensuel   DOUBLE PRECISION,
    url_pdf         VARCHAR(500),
    bien_id         BIGINT,
    proprietaire_id BIGINT,
    locataire_id    BIGINT,

    CONSTRAINT fk_contrats_bien          FOREIGN KEY (bien_id)         REFERENCES biens(id),
    CONSTRAINT fk_contrats_proprietaire  FOREIGN KEY (proprietaire_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_contrats_locataire     FOREIGN KEY (locataire_id)    REFERENCES utilisateurs(id)
);

-- Index sur les FK (JOINs fréquents)
CREATE INDEX IF NOT EXISTS idx_contrats_bien_id         ON contrats(bien_id);
CREATE INDEX IF NOT EXISTS idx_contrats_proprietaire_id ON contrats(proprietaire_id);
CREATE INDEX IF NOT EXISTS idx_contrats_locataire_id    ON contrats(locataire_id);

-- Index sur les dates (filtres par période)
CREATE INDEX IF NOT EXISTS idx_contrats_dates ON contrats(date_debut, date_fin);