-- =====================================================
-- Migration table quittances
-- Alignée avec l'entité Quittance.java
-- =====================================================

CREATE TABLE IF NOT EXISTS quittances (
    id           BIGSERIAL PRIMARY KEY,
    montant      DOUBLE PRECISION,
    mois         VARCHAR(20),
    annee        INTEGER,
    url_pdf      VARCHAR(500),
    bien_id      BIGINT,
    locataire_id BIGINT,

    CONSTRAINT fk_quittances_bien     FOREIGN KEY (bien_id)      REFERENCES biens(id),
    CONSTRAINT fk_quittances_locataire FOREIGN KEY (locataire_id) REFERENCES utilisateurs(id)
);

-- Index sur FK
CREATE INDEX IF NOT EXISTS idx_quittances_bien_id      ON quittances(bien_id);
CREATE INDEX IF NOT EXISTS idx_quittances_locataire_id ON quittances(locataire_id);

-- Index composite mois + annee (recherche par période)
CREATE INDEX IF NOT EXISTS idx_quittances_periode ON quittances(annee, mois);
