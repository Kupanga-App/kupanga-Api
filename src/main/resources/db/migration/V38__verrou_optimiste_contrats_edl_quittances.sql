-- B6 : verrou optimiste (@Version) sur les documents signés.
-- Sans lui, une relance du propriétaire pendant la signature du locataire pouvait ramener un contrat SIGNE
-- en attente de signature (la dernière écriture gagnait).
ALTER TABLE contrats         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE etats_des_lieux  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE quittances       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
