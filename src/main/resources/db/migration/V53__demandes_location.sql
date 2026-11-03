-- Demandes de location : le propriétaire propose son bien à un candidat ; le bien n'est assigné
-- qu'à l'acceptation par le locataire (valable 72 h).
CREATE TABLE demandes_location
(
    id               BIGSERIAL    PRIMARY KEY,
    bien_id          BIGINT       NOT NULL REFERENCES biens (id),
    proprietaire_id  BIGINT       NOT NULL REFERENCES utilisateurs (id),
    locataire_id     BIGINT       NOT NULL REFERENCES utilisateurs (id),
    statut           VARCHAR(20)  NOT NULL,
    date_creation    TIMESTAMP    NOT NULL,
    date_expiration  TIMESTAMP    NOT NULL,
    date_reponse     TIMESTAMP,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_demandes_location_statut
        CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE', 'EXPIREE')),
    CONSTRAINT ck_demandes_location_expiration CHECK (date_expiration > date_creation)
);

-- Une seule demande en attente par bien
CREATE UNIQUE INDEX ux_demandes_location_une_en_attente
    ON demandes_location (bien_id) WHERE statut = 'EN_ATTENTE';

CREATE INDEX ix_demandes_location_locataire ON demandes_location (locataire_id, statut);
CREATE INDEX ix_demandes_location_bien ON demandes_location (bien_id, date_creation DESC);
