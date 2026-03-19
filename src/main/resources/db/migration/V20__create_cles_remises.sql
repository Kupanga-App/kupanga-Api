-- ============================================================
-- V20 — Création de la table cles_remises
--       Clés remises / restituées lors d'un état des lieux
-- ============================================================

CREATE TABLE cles_remises (
    id                  BIGSERIAL       PRIMARY KEY,
    type_cle            VARCHAR(100)    NOT NULL,   -- ex: "Porte d'entrée"
    quantite            INTEGER         NOT NULL    CHECK (quantite > 0),
    etat_des_lieux_id   BIGINT          NOT NULL
        REFERENCES etats_des_lieux(id) ON DELETE CASCADE
);

CREATE INDEX idx_cles_etat ON cles_remises(etat_des_lieux_id);
