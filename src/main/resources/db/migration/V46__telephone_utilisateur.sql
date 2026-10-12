-- C6 : numéro de téléphone de contact, facultatif et non vérifié (pas un identifiant de connexion).
-- Stocké au format E.164 (+243812345678), normalisé par l'application (TelephoneUtils).
ALTER TABLE utilisateurs
    ADD COLUMN IF NOT EXISTS telephone VARCHAR(16);

ALTER TABLE utilisateurs
    ADD CONSTRAINT ck_utilisateurs_telephone_e164
        CHECK (telephone IS NULL OR telephone ~ '^\+[1-9][0-9]{7,14}$');
