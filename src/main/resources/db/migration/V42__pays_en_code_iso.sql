-- =====================================================
-- J1 : biens.pays passe du texte libre (« France ») au code ISO 3166-1 alpha-2 (FR, BE, CD, CG),
-- lu par l'enum Pays. Écritures courantes reconnues sans tenir compte de la casse, des accents ni des espaces.
-- Une valeur non reconnue (ou vide) arrête la migration : la corriger à la main avant de relancer. Pour lister :
--   SELECT pays, count(*) FROM biens GROUP BY pays;
-- =====================================================

UPDATE biens
SET pays = CASE
    WHEN lower(translate(trim(pays), 'ÉÈÊéèêÔô-', 'EEEeeeOo ')) IN ('fr', 'france')
        THEN 'FR'
    WHEN lower(translate(trim(pays), 'ÉÈÊéèêÔô-', 'EEEeeeOo ')) IN ('be', 'belgique', 'belgium')
        THEN 'BE'
    WHEN lower(translate(trim(pays), 'ÉÈÊéèêÔô-', 'EEEeeeOo ')) IN ('cd', 'rdc', 'drc', 'congo kinshasa',
            'republique democratique du congo', 'rd congo')
        THEN 'CD'
    WHEN lower(translate(trim(pays), 'ÉÈÊéèêÔô-', 'EEEeeeOo ')) IN ('cg', 'congo brazzaville',
            'republique du congo')
        THEN 'CG'
    ELSE pays
END;

DO $$
DECLARE
    nb_inconnus INTEGER;
BEGIN
    SELECT count(*) INTO nb_inconnus
    FROM biens
    WHERE pays IS NULL OR pays NOT IN ('FR', 'BE', 'CD', 'CG');

    IF nb_inconnus > 0 THEN
        RAISE EXCEPTION 'J1 : % bien(s) avec un pays non reconnu ou vide, à corriger avant migration (requête en tête de V42)', nb_inconnus;
    END IF;
END $$;

ALTER TABLE biens ALTER COLUMN pays TYPE VARCHAR(2);
ALTER TABLE biens ALTER COLUMN pays SET NOT NULL;
ALTER TABLE biens ADD CONSTRAINT ck_biens_pays CHECK (pays IN ('FR', 'BE', 'CD', 'CG'));
