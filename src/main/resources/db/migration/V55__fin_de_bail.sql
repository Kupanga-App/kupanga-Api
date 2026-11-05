-- Fin de bail (2026-11-05) : congé donné par le propriétaire ou le locataire avec une date de fin, puis fin de la
-- location enregistrée par le propriétaire avec le dépôt de garantie restitué (et le motif d'une retenue).
ALTER TABLE locations
    ADD COLUMN date_fin_prevue  DATE,
    ADD COLUMN conge_donne_par  VARCHAR(20),
    ADD COLUMN date_conge       TIMESTAMP,
    ADD COLUMN depot_restitue   NUMERIC(12, 2),
    ADD COLUMN motif_retenue    VARCHAR(500);

ALTER TABLE locations
    ADD CONSTRAINT ck_locations_conge_donne_par
        CHECK (conge_donne_par IS NULL OR conge_donne_par IN ('PROPRIETAIRE', 'LOCATAIRE')),
    ADD CONSTRAINT ck_locations_conge_complet
        CHECK ((date_fin_prevue IS NULL) = (conge_donne_par IS NULL)),
    ADD CONSTRAINT ck_locations_depot_restitue CHECK (depot_restitue IS NULL OR depot_restitue >= 0);
