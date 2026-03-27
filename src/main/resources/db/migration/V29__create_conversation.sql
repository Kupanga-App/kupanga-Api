-- 1. Création de la table conversations
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,

    -- ─── Bien concerné ───────────────────────────────
    bien_id BIGINT,

    -- ─── Dernier message ─────────────────────────────
    last_message TEXT,
    last_message_at TIMESTAMP,

    -- ─── Audit ───────────────────────────────────────
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Clé étrangère vers bien
ALTER TABLE conversations
ADD CONSTRAINT fk_conversation_bien
FOREIGN KEY (bien_id)
REFERENCES biens(id)
ON DELETE SET NULL;