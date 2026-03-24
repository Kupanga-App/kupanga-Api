-- V13__create_bien_poi.sql
CREATE TABLE bien_poi (
    id             BIGSERIAL    PRIMARY KEY,
    bien_id        BIGINT       NOT NULL REFERENCES biens(id) ON DELETE CASCADE,
    poi_type       VARCHAR(50)  NOT NULL,
    present        BOOLEAN      NOT NULL,
    rayon_metres   DOUBLE PRECISION NOT NULL,
    nombre_trouve  INTEGER      NOT NULL DEFAULT 0,
    calcule_le     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_bien_poi UNIQUE (bien_id, poi_type)  -- un seul enregistrement par bien/type
);

CREATE INDEX idx_bien_poi_bien_id ON bien_poi (bien_id);
CREATE INDEX idx_bien_poi_present ON bien_poi (bien_id, poi_type, present);