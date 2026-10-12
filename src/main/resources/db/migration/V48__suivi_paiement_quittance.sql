-- C8-bis : suivi manuel des paiements. Le propriétaire déclare comment le loyer a été reçu (hors application) ;
-- photo du reçu facultative dans un bucket MinIO privé (clé seulement en base). Aucun paiement via l'application.
ALTER TABLE quittances
    ADD COLUMN IF NOT EXISTS mode_paiement      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS reference_paiement VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cle_recu           VARCHAR(255);

ALTER TABLE quittances
    ADD CONSTRAINT ck_quittances_mode_paiement
        CHECK (mode_paiement IS NULL OR mode_paiement IN ('ESPECES', 'MOBILE_MONEY', 'VIREMENT', 'CHEQUE', 'AUTRE'));
