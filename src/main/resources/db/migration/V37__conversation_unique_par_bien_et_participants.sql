-- =====================================================
-- W1 : une seule conversation par bien et par paire de participants
-- (sinon la recherche de la conversation renvoie plusieurs lignes → erreur 500).
-- =====================================================

-- 1. Arrêt si des doublons existent déjà : ils doivent être fusionnés à la main
--    (messages rattachés à une seule conversation) avant de relancer la migration. Pour les lister :
--      SELECT bien_id, least(email_expediteur, email_destinataire), greatest(email_expediteur, email_destinataire),
--             array_agg(id)
--      FROM conversations GROUP BY 1, 2, 3 HAVING count(*) > 1;
DO $$
DECLARE
    nb_doublons INTEGER;
BEGIN
    SELECT count(*)
    INTO nb_doublons
    FROM (
        SELECT 1
        FROM conversations
        GROUP BY bien_id, least(email_expediteur, email_destinataire), greatest(email_expediteur, email_destinataire)
        HAVING count(*) > 1
    ) d;

    IF nb_doublons > 0 THEN
        RAISE EXCEPTION 'W1 : % conversation(s) en double (même bien, mêmes participants), à fusionner avant migration (requête en tête de V37)', nb_doublons;
    END IF;
END $$;

-- 2. Unicité quel que soit le sens (expéditeur / destinataire du premier message)
CREATE UNIQUE INDEX IF NOT EXISTS ux_conversations_bien_participants
    ON conversations (bien_id, least(email_expediteur, email_destinataire), greatest(email_expediteur, email_destinataire));
