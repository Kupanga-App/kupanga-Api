-- J4 (C1) : adresse congolaise. Champs facultatifs en base : leur caractère obligatoire ou masqué dépend du pays
-- du bien (kupanga.juridictions.<PAYS>.champs-obligatoires / champs-masques), contrôlé par l'application.
-- code_postal est déjà facultatif (V3) : il n'est obligatoire qu'en France.
ALTER TABLE biens
    ADD COLUMN IF NOT EXISTS commune          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS quartier         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS avenue           VARCHAR(150),
    ADD COLUMN IF NOT EXISTS numero_parcelle  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS point_de_repere  VARCHAR(255);
