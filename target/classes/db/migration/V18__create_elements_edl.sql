-- ============================================================
-- V18 — Création de la table elements_edl
--       Un élément appartient à une pièce
-- ============================================================

CREATE TABLE elements_edl (
    id              BIGSERIAL       PRIMARY KEY,
    type_element    VARCHAR(50)     NOT NULL,   -- enum TypeElement
    etat_element    VARCHAR(50)     NOT NULL,   -- enum EtatElement
    description     VARCHAR(255),
    observation     TEXT,
    piece_id        BIGINT          NOT NULL
        REFERENCES pieces_edl(id) ON DELETE CASCADE
);

CREATE INDEX idx_elements_edl_piece ON elements_edl(piece_id);
