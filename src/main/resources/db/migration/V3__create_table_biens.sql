-- =====================================================
-- Migration table biens
-- Création initiale alignée avec l'entité Bien.java
-- =====================================================

CREATE TABLE IF NOT EXISTS biens (
    id               BIGSERIAL PRIMARY KEY,
    titre            VARCHAR(255),
    adresse          VARCHAR(255),
    ville            VARCHAR(255),
    code_postal      VARCHAR(20),
    pays             VARCHAR(100),
    description      TEXT,
    type_bien        VARCHAR(50),
    localisation     GEOMETRY(Point, 4326),
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    proprietaire_id  BIGINT,
    locataire_id     BIGINT,

    CONSTRAINT fk_biens_proprietaire FOREIGN KEY (proprietaire_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_biens_locataire    FOREIGN KEY (locataire_id)    REFERENCES utilisateurs(id)
);

CREATE INDEX IF NOT EXISTS idx_biens_localisation ON biens USING GIST(localisation);