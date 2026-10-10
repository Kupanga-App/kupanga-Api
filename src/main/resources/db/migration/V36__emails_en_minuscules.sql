-- =====================================================
-- A10 : e-mails insensibles à la casse
-- Les e-mails sont stockés en minuscules (sans espaces autour) ;
-- unicité garantie sur lower(email).
-- =====================================================

-- 1. Arrêt si deux comptes deviennent identiques une fois en minuscules : ils doivent être fusionnés
--    à la main avant de relancer la migration. Le message ne contient pas les adresses (logs, Sentry) ;
--    pour les lister, en base :
--      SELECT lower(trim(email)), array_agg(id) FROM utilisateurs
--      GROUP BY 1 HAVING count(*) > 1;
--    Les adresses non ASCII sont signalées : lower() de PostgreSQL dépend alors de la collation et peut
--    différer de la normalisation Java (EmailUtils) ; les lister avec
--      SELECT id, email FROM utilisateurs WHERE octet_length(email) <> char_length(email);
DO $$
DECLARE
    nb_doublons INTEGER;
    nb_non_ascii INTEGER;
BEGIN
    SELECT count(*)
    INTO nb_doublons
    FROM (
        SELECT 1
        FROM utilisateurs
        WHERE email IS NOT NULL
        GROUP BY lower(trim(email))
        HAVING count(*) > 1
    ) d;

    IF nb_doublons > 0 THEN
        RAISE EXCEPTION 'A10 : % adresse(s) e-mail en double une fois en minuscules, comptes à fusionner avant migration (requête en tête de V36)', nb_doublons;
    END IF;

    SELECT count(*) INTO nb_non_ascii FROM utilisateurs WHERE octet_length(email) <> char_length(email);
    IF nb_non_ascii > 0 THEN
        RAISE WARNING 'A10 : % adresse(s) e-mail non ASCII, à vérifier après migration (requête en tête de V36)', nb_non_ascii;
    END IF;
END $$;

-- 2. Passage en minuscules
UPDATE utilisateurs
SET email = lower(trim(email))
WHERE email IS NOT NULL AND email <> lower(trim(email));

UPDATE conversations
SET email_expediteur = lower(trim(email_expediteur))
WHERE email_expediteur IS NOT NULL AND email_expediteur <> lower(trim(email_expediteur));

UPDATE conversations
SET email_destinataire = lower(trim(email_destinataire))
WHERE email_destinataire IS NOT NULL AND email_destinataire <> lower(trim(email_destinataire));

-- 3. Unicité insensible à la casse (la contrainte UNIQUE (email) reste : elle sert aux recherches exactes)
CREATE UNIQUE INDEX IF NOT EXISTS ux_utilisateurs_email_lower ON utilisateurs (lower(email));
