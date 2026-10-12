-- =====================================================
-- J3 (CLAUDE.md §4bis, B13, C4) :
--  * devise du bien (EUR, USD, CDF, XAF) ;
--  * montants en NUMERIC(12,2) au lieu de DOUBLE PRECISION (plus d'erreurs d'arrondi) ;
--  * contrats et quittances figent pays, devise et version du modèle de document à leur création.
-- Données existantes : devise déduite du pays (seule la France est en base au 2026-10-09),
-- modèle « <pays>-v1 ». Arrêt si un contrat ou une quittance n'a pas de bien (pays impossible à déduire).
-- =====================================================

-- ─── Biens ────────────────────────────────────────────────────────────────────
ALTER TABLE biens ADD COLUMN IF NOT EXISTS devise VARCHAR(3);
UPDATE biens SET devise = CASE pays WHEN 'CD' THEN 'USD' WHEN 'CG' THEN 'XAF' ELSE 'EUR' END
WHERE devise IS NULL;
ALTER TABLE biens ALTER COLUMN devise SET NOT NULL;
ALTER TABLE biens ADD CONSTRAINT ck_biens_devise CHECK (devise IN ('EUR', 'USD', 'CDF', 'XAF'));

ALTER TABLE biens
    ALTER COLUMN loyer_mensuel      TYPE NUMERIC(12, 2) USING round(loyer_mensuel::numeric, 2),
    ALTER COLUMN charges_mensuelles TYPE NUMERIC(12, 2) USING round(charges_mensuelles::numeric, 2),
    ALTER COLUMN depot_garantie     TYPE NUMERIC(12, 2) USING round(depot_garantie::numeric, 2);

-- ─── Contrôle : chaque contrat et chaque quittance a un bien ─────────────────
DO $$
DECLARE
    nb_orphelins INTEGER;
BEGIN
    SELECT (SELECT count(*) FROM contrats   WHERE bien_id IS NULL)
         + (SELECT count(*) FROM quittances WHERE bien_id IS NULL)
    INTO nb_orphelins;
    IF nb_orphelins > 0 THEN
        RAISE EXCEPTION 'J3 : % contrat(s) ou quittance(s) sans bien, à rattacher avant migration', nb_orphelins;
    END IF;
END $$;

-- ─── Contrats ─────────────────────────────────────────────────────────────────
ALTER TABLE contrats
    ADD COLUMN IF NOT EXISTS pays           VARCHAR(2),
    ADD COLUMN IF NOT EXISTS devise         VARCHAR(3),
    ADD COLUMN IF NOT EXISTS modele_version VARCHAR(20);
UPDATE contrats c
SET pays = b.pays, devise = b.devise, modele_version = lower(b.pays) || '-v1'
FROM biens b
WHERE b.id = c.bien_id AND c.pays IS NULL;
ALTER TABLE contrats
    ALTER COLUMN pays           SET NOT NULL,
    ALTER COLUMN devise         SET NOT NULL,
    ALTER COLUMN modele_version SET NOT NULL,
    ALTER COLUMN loyer_mensuel      TYPE NUMERIC(12, 2) USING round(loyer_mensuel::numeric, 2),
    ALTER COLUMN charges_mensuelles TYPE NUMERIC(12, 2) USING round(charges_mensuelles::numeric, 2),
    ALTER COLUMN depot_garantie     TYPE NUMERIC(12, 2) USING round(depot_garantie::numeric, 2);
ALTER TABLE contrats ADD CONSTRAINT ck_contrats_pays   CHECK (pays IN ('FR', 'BE', 'CD', 'CG'));
ALTER TABLE contrats ADD CONSTRAINT ck_contrats_devise CHECK (devise IN ('EUR', 'USD', 'CDF', 'XAF'));

-- ─── Quittances (juridiction du contrat si rattachée, sinon du bien) ─────────
ALTER TABLE quittances
    ADD COLUMN IF NOT EXISTS pays           VARCHAR(2),
    ADD COLUMN IF NOT EXISTS devise         VARCHAR(3),
    ADD COLUMN IF NOT EXISTS modele_version VARCHAR(20);
UPDATE quittances q
SET pays = c.pays, devise = c.devise, modele_version = c.modele_version
FROM contrats c
WHERE c.id = q.contrat_id AND q.pays IS NULL;
UPDATE quittances q
SET pays = b.pays, devise = b.devise, modele_version = lower(b.pays) || '-v1'
FROM biens b
WHERE b.id = q.bien_id AND q.pays IS NULL;
ALTER TABLE quittances
    ALTER COLUMN pays           SET NOT NULL,
    ALTER COLUMN devise         SET NOT NULL,
    ALTER COLUMN modele_version SET NOT NULL,
    ALTER COLUMN loyer_mensuel      TYPE NUMERIC(12, 2) USING round(loyer_mensuel::numeric, 2),
    ALTER COLUMN charges_mensuelles TYPE NUMERIC(12, 2) USING round(charges_mensuelles::numeric, 2),
    ALTER COLUMN montant_total      TYPE NUMERIC(12, 2) USING round(montant_total::numeric, 2);
ALTER TABLE quittances ADD CONSTRAINT ck_quittances_pays   CHECK (pays IN ('FR', 'BE', 'CD', 'CG'));
ALTER TABLE quittances ADD CONSTRAINT ck_quittances_devise CHECK (devise IN ('EUR', 'USD', 'CDF', 'XAF'));
