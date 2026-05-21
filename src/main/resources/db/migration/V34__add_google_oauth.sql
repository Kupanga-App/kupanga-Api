ALTER TABLE utilisateurs ADD COLUMN google_id VARCHAR(255);
ALTER TABLE utilisateurs ALTER COLUMN mot_de_passe DROP NOT NULL;

CREATE INDEX idx_utilisateurs_google_id ON utilisateurs (google_id);
