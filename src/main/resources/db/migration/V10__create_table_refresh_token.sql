-- =====================================================
-- Migration table refreshToken
-- Alignée avec l'entité RefreshToken.java
-- =====================================================

CREATE TABLE IF NOT EXISTS refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(500),
    expiration  TIMESTAMP,
    revoked     BOOLEAN DEFAULT FALSE,
    user_id     BIGINT NOT NULL UNIQUE,     -- UNIQUE car @OneToOne

    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id)
);

-- Index sur token (lookup fréquent pour valider le refresh)
CREATE INDEX IF NOT EXISTS idx_refresh_token       ON refresh_token(token);
-- Index sur user_id
CREATE INDEX IF NOT EXISTS idx_refresh_token_user  ON refresh_token(user_id);
-- Index sur revoked (filtre tokens révoqués)
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON refresh_token(revoked);