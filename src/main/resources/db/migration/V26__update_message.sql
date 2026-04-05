-- ============================================================
-- V26 — Update table messages (évolution messagerie temps réel)
-- ============================================================

-- 1. Renommer date_envoi -> created_at (standardisation)
ALTER TABLE messages
    RENAME COLUMN date_envoi TO created_at;

-- 2. Rendre les champs obligatoires (cohérence métier)
ALTER TABLE messages
    ALTER COLUMN contenu SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN expediteur_id SET NOT NULL,
    ALTER COLUMN destinataire_id SET NOT NULL;

-- 3. Valeur par défaut pour created_at
ALTER TABLE messages
    ALTER COLUMN created_at SET DEFAULT now();

-- 4. Nettoyage ancien index (si existant)
DROP INDEX IF EXISTS idx_messages_date_envoi;

-- 5. Nouvel index pour tri performant
CREATE INDEX IF NOT EXISTS idx_messages_created_at
    ON messages(created_at DESC);

-- 6. Index conversation optimisé (important pour chat)
CREATE INDEX IF NOT EXISTS idx_messages_conversation
    ON messages(expediteur_id, destinataire_id, created_at DESC);

-- 7. Index lecture rapide messages non lus
CREATE INDEX IF NOT EXISTS idx_messages_destinataire_lu
    ON messages(destinataire_id, lu);