-- C2 : origine de la position d'un bien (repère sur la carte, adresse géocodée, centre du quartier, de la commune
-- ou de la ville, aucune). Le géocodage n'est plus obligatoire à la création (B10).
-- Biens existants : tous créés avec une adresse géocodée (création refusée sinon jusqu'ici).
ALTER TABLE biens
    ADD COLUMN IF NOT EXISTS precision_localisation VARCHAR(10);

UPDATE biens
SET precision_localisation = CASE WHEN localisation IS NULL THEN 'AUCUNE' ELSE 'ADRESSE' END
WHERE precision_localisation IS NULL;

ALTER TABLE biens
    ALTER COLUMN precision_localisation SET NOT NULL,
    ALTER COLUMN precision_localisation SET DEFAULT 'AUCUNE',
    ADD CONSTRAINT ck_biens_precision_localisation
        CHECK (precision_localisation IN ('REPERE', 'ADRESSE', 'QUARTIER', 'COMMUNE', 'VILLE', 'AUCUNE'));
