-- ============================================================
-- V23 — Refonte de la table quittances
-- ============================================================

-- Ajout de toutes les colonnes (mois et annee recréés en INTEGER)
ALTER TABLE quittances
    ADD COLUMN IF NOT EXISTS mois                INTEGER,
    ADD COLUMN IF NOT EXISTS annee               INTEGER,
    ADD COLUMN IF NOT EXISTS url_pdf             VARCHAR(500),
    ADD COLUMN IF NOT EXISTS loyer_mensuel       DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS charges_mensuelles  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS montant_total        DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS date_paiement        DATE,
    ADD COLUMN IF NOT EXISTS date_echeance        DATE,
    ADD COLUMN IF NOT EXISTS statut               VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    ADD COLUMN IF NOT EXISTS proprietaire_id      BIGINT REFERENCES utilisateurs(id),
    ADD COLUMN IF NOT EXISTS contrat_id           BIGINT REFERENCES contrats(id),
    ADD COLUMN IF NOT EXISTS created_at           TIMESTAMP DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at           TIMESTAMP DEFAULT now();

-- Contraintes NOT NULL (colonnes qui existent déjà + celles qu'on vient d'ajouter)
ALTER TABLE quittances
    ALTER COLUMN bien_id       SET NOT NULL,
    ALTER COLUMN locataire_id  SET NOT NULL;

-- Index utiles
CREATE INDEX IF NOT EXISTS idx_quittances_bien       ON quittances(bien_id);
CREATE INDEX IF NOT EXISTS idx_quittances_locataire  ON quittances(locataire_id);
CREATE INDEX IF NOT EXISTS idx_quittances_contrat    ON quittances(contrat_id);
CREATE INDEX IF NOT EXISTS idx_quittances_statut     ON quittances(statut);
CREATE INDEX IF NOT EXISTS idx_quittances_mois_annee ON quittances(annee, mois);