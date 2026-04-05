-- 1. Ajouter la colonne NOT NULL
ALTER TABLE messages
ADD COLUMN conversation_id BIGINT NOT NULL;

-- 2. Ajouter la clé étrangère
ALTER TABLE messages
ADD CONSTRAINT fk_message_conversation
FOREIGN KEY (conversation_id)
REFERENCES conversations(id)
ON DELETE CASCADE;

-- 3. Index pour les performances
CREATE INDEX idx_message_conversation_id
ON messages(conversation_id);