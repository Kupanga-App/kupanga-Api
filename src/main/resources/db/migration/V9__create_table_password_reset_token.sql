-- =====================================================
-- Migration table password_reset_token
-- Alignée avec l'entité PasswordResetToken.java
-- =====================================================

CREATE TABLE IF NOT EXISTS password_reset_token (
    id              BIGSERIAL PRIMARY KEY,
    token           VARCHAR(500) NOT NULL UNIQUE,
    expiration_date TIMESTAMP,
    user_id         BIGINT UNIQUE,          -- UNIQUE car @OneToOne

    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id)
);

-- Index sur token (lookup fréquent pour valider le lien de reset)
CREATE INDEX IF NOT EXISTS idx_prt_token   ON password_reset_token(token);
-- Index sur user_id
CREATE INDEX IF NOT EXISTS idx_prt_user_id ON password_reset_token(user_id);