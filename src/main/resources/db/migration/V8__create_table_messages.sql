-- =====================================================
-- Migration table messages
-- Alignée avec l'entité Message.java
-- =====================================================

CREATE TABLE IF NOT EXISTS messages (
    id              BIGSERIAL PRIMARY KEY,
    contenu         TEXT,
    date_envoi      TIMESTAMP,
    lu              BOOLEAN DEFAULT FALSE,
    expediteur_id   BIGINT,
    destinataire_id BIGINT,
    bien_id         BIGINT,

    CONSTRAINT fk_messages_expediteur   FOREIGN KEY (expediteur_id)   REFERENCES utilisateurs(id),
    CONSTRAINT fk_messages_destinataire FOREIGN KEY (destinataire_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_messages_bien         FOREIGN KEY (bien_id)         REFERENCES biens(id)
);

-- Index sur FK (JOINs fréquents)
CREATE INDEX IF NOT EXISTS idx_messages_expediteur_id   ON messages(expediteur_id);
CREATE INDEX IF NOT EXISTS idx_messages_destinataire_id ON messages(destinataire_id);
CREATE INDEX IF NOT EXISTS idx_messages_bien_id         ON messages(bien_id);

-- Index sur lu (filtre messages non lus fréquent)
CREATE INDEX IF NOT EXISTS idx_messages_lu ON messages(lu);

-- Index sur date_envoi (tri chronologique)
CREATE INDEX IF NOT EXISTS idx_messages_date_envoi ON messages(date_envoi);

-- Index composite destinataire + lu (cas d'usage : "mes messages non lus")
CREATE INDEX IF NOT EXISTS idx_messages_destinataire_lu ON messages(destinataire_id, lu);
