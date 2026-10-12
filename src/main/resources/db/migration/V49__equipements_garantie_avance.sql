-- C10 à C13 (RDC) : nouveaux types de bien (texte, sans contrainte : liste filtrée par le profil du pays),
-- équipements et heures de courant (C11), garantie en mois et loyers payés d'avance (C12), sur le bien et figés
-- sur le bail ; nouveaux types de compteurs d'état des lieux (C13 : colonne texte existante, rien à migrer).

ALTER TABLE biens
    ADD COLUMN IF NOT EXISTS garantie_mois               INTEGER,
    ADD COLUMN IF NOT EXISTS avance_mois                 INTEGER,
    ADD COLUMN IF NOT EXISTS heures_electricite_par_jour INTEGER;

ALTER TABLE biens
    ADD CONSTRAINT ck_biens_garantie_mois CHECK (garantie_mois IS NULL OR garantie_mois BETWEEN 0 AND 24),
    ADD CONSTRAINT ck_biens_avance_mois   CHECK (avance_mois IS NULL OR avance_mois BETWEEN 0 AND 24),
    ADD CONSTRAINT ck_biens_heures_elec   CHECK (heures_electricite_par_jour IS NULL OR heures_electricite_par_jour BETWEEN 0 AND 24);

-- Parcelle ou terrain : sans surface habitable ni pièces (déjà facultatives en base)

ALTER TABLE contrats
    ADD COLUMN IF NOT EXISTS garantie_mois INTEGER,
    ADD COLUMN IF NOT EXISTS avance_mois   INTEGER;

CREATE TABLE IF NOT EXISTS bien_equipements (
    bien_id    BIGINT      NOT NULL REFERENCES biens (id),
    equipement VARCHAR(30) NOT NULL,
    PRIMARY KEY (bien_id, equipement),
    CONSTRAINT ck_bien_equipements CHECK (equipement IN (
        'EAU_REGIDESO', 'FORAGE', 'CITERNE', 'ELECTRICITE_SNEL', 'GROUPE_ELECTROGENE', 'PANNEAUX_SOLAIRES',
        'CLOTURE', 'GARDIENNAGE', 'PARKING', 'CLIMATISATION'))
);
