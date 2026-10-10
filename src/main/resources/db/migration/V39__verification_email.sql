-- =====================================================
-- A14 : vérification de l'adresse e-mail à l'inscription
-- =====================================================

-- Comptes existants (créés avant A14) et comptes Google : considérés comme vérifiés
ALTER TABLE utilisateurs ADD COLUMN email_verifie BOOLEAN NOT NULL DEFAULT TRUE;
-- Nouveaux comptes : non vérifiés tant que le lien envoyé par e-mail n'a pas été ouvert
ALTER TABLE utilisateurs ALTER COLUMN email_verifie SET DEFAULT FALSE;

-- Un jeton par compte (le renvoi remplace l'ancien), valable 24 h
CREATE TABLE jeton_verification_email (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expiration  TIMESTAMP   NOT NULL,
    user_id     BIGINT      NOT NULL UNIQUE,

    CONSTRAINT fk_jve_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE
);
