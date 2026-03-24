-- V21__fix_compteur_releves_index_type.sql

ALTER TABLE compteur_releves
    ALTER COLUMN index TYPE DOUBLE PRECISION
    USING index::DOUBLE PRECISION;