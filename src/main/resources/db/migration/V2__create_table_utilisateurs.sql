-- =====================================================
-- Migration table utilisateurs
-- Création initiale alignée avec l'entité User.java
-- =====================================================

CREATE TABLE IF NOT EXISTS utilisateurs (
    id                  BIGSERIAL PRIMARY KEY,
    prenom              VARCHAR(255),
    nom                 VARCHAR(255),
    email               VARCHAR(255) UNIQUE,
    "motDePasse"        VARCHAR(255),
    role                VARCHAR(50),
    "A_completer_profil" BOOLEAN DEFAULT FALSE,
    url_photo_profil    VARCHAR(500)
);

-- Index sur les colonnes fréquemment utilisées en recherche/filtre
CREATE INDEX IF NOT EXISTS idx_utilisateurs_role        ON utilisateurs(role);
CREATE INDEX IF NOT EXISTS idx_utilisateurs_nom_prenom  ON utilisateurs(nom, prenom);