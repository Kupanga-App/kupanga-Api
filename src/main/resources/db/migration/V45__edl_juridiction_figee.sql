-- J5 : comme les contrats et les quittances (V43), l'état des lieux fige le pays et la version du modèle de document
-- à sa création : un EDL régénéré (signature) garde le modèle avec lequel il a été établi.
ALTER TABLE etats_des_lieux
    ADD COLUMN IF NOT EXISTS pays           VARCHAR(2),
    ADD COLUMN IF NOT EXISTS modele_version VARCHAR(20);

-- EDL existants : pays du bien et première version du modèle de ce pays (seule version existante avant J5)
UPDATE etats_des_lieux e
SET pays = b.pays, modele_version = lower(b.pays) || '-v1'
FROM biens b
WHERE e.bien_id = b.id
  AND e.pays IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM etats_des_lieux WHERE pays IS NULL) THEN
        RAISE EXCEPTION 'V45 : état(s) des lieux sans bien, pays impossible à déterminer';
    END IF;
END $$;

ALTER TABLE etats_des_lieux
    ALTER COLUMN pays SET NOT NULL,
    ALTER COLUMN modele_version SET NOT NULL;

ALTER TABLE etats_des_lieux ADD CONSTRAINT ck_etats_des_lieux_pays CHECK (pays IN ('FR', 'BE', 'CD', 'CG'));
