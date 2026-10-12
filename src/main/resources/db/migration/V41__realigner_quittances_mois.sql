-- =====================================================
-- D6 : V24 a été réécrite après coup (1re version : mois VARCHAR → INTEGER ; version actuelle : → VARCHAR(20)).
-- Une base qui a joué la 1re version garde mois en INTEGER, alors que l'entité Quittance attend un texte :
-- `flyway repair` ne réaligne que les checksums, cette migration réaligne la colonne. Sans effet ailleurs.
-- =====================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'quittances'
          AND column_name  = 'mois'
          AND data_type    = 'integer'
    ) THEN
        ALTER TABLE quittances ALTER COLUMN mois TYPE VARCHAR(20) USING mois::VARCHAR;
    END IF;
END $$;
