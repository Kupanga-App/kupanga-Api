-- =====================================================
-- B12 : plus de suppression en cascade des baux
--  * un bien n'est plus supprimé, il est archivé (lecture seule, hors recherche publique) ;
--  * un compte qui a des données liées (bien, contrat, quittance, EDL) est anonymisé au lieu d'être supprimé.
-- =====================================================

ALTER TABLE biens
    ADD COLUMN IF NOT EXISTS archive        BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS date_archivage TIMESTAMP;

ALTER TABLE utilisateurs
    ADD COLUMN IF NOT EXISTS anonymise          BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS date_anonymisation TIMESTAMP;
