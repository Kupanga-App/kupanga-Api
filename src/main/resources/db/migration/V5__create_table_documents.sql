-- =====================================================
-- Migration table documents
-- Alignée avec l'entité Document.java
-- =====================================================

CREATE TABLE IF NOT EXISTS documents (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(255),
    url         VARCHAR(500),
    date_ajout  DATE,
    bien_id     BIGINT,

    CONSTRAINT fk_documents_bien FOREIGN KEY (bien_id) REFERENCES biens(id)
);

-- Index sur FK
CREATE INDEX IF NOT EXISTS idx_documents_bien_id ON documents(bien_id);

-- Index sur nom (recherche par nom de document)
CREATE INDEX IF NOT EXISTS idx_documents_nom ON documents(nom);