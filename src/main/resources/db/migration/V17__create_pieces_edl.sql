-- ============================================================
-- V17 — Création de la table pieces_edl
--       Une pièce appartient à un état des lieux
-- ============================================================

CREATE TABLE pieces_edl (
    id                  BIGSERIAL       PRIMARY KEY,
    nom_piece           VARCHAR(100)    NOT NULL,
    ordre               INTEGER,
    observations        TEXT,
    etat_des_lieux_id   BIGINT          NOT NULL
        REFERENCES etats_des_lieux(id) ON DELETE CASCADE
);

CREATE INDEX idx_pieces_edl_etat ON pieces_edl(etat_des_lieux_id);
