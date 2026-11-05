-- Locations : relation entre un bien et son locataire, de la réservation à la fin du bail (décision du 2026-11-05).
-- Accepter une demande de location RÉSERVE le bien (14 jours) ; le candidat devient locataire (ACTIVE)
-- à la signature du bail. biens.locataire_id reste le locataire actuel (location ACTIVE).
CREATE TABLE locations
(
    id                BIGSERIAL    PRIMARY KEY,
    bien_id           BIGINT       NOT NULL REFERENCES biens (id),
    proprietaire_id   BIGINT       NOT NULL REFERENCES utilisateurs (id),
    locataire_id      BIGINT       NOT NULL REFERENCES utilisateurs (id),
    demande_id        BIGINT       REFERENCES demandes_location (id),
    statut            VARCHAR(20)  NOT NULL,
    date_reservation  TIMESTAMP    NOT NULL,
    date_expiration   TIMESTAMP    NOT NULL,
    date_debut        TIMESTAMP,
    date_fin          TIMESTAMP,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_locations_statut
        CHECK (statut IN ('RESERVEE', 'ACTIVE', 'TERMINEE', 'DESISTEE', 'ANNULEE', 'EXPIREE')),
    CONSTRAINT ck_locations_expiration CHECK (date_expiration > date_reservation)
);

-- Au plus une location réservée ou en cours par bien
CREATE UNIQUE INDEX ux_locations_une_en_cours
    ON locations (bien_id) WHERE statut IN ('RESERVEE', 'ACTIVE');

CREATE INDEX ix_locations_locataire ON locations (locataire_id, statut);
CREATE INDEX ix_locations_reservees_echeance ON locations (date_expiration) WHERE statut = 'RESERVEE';

-- Locataires déjà assignés (ancien parcours) : location en cours
INSERT INTO locations (bien_id, proprietaire_id, locataire_id, statut, date_reservation, date_expiration, date_debut)
SELECT b.id, b.proprietaire_id, b.locataire_id, 'ACTIVE', now(), now() + INTERVAL '14 days', now()
FROM biens b
WHERE b.locataire_id IS NOT NULL;
