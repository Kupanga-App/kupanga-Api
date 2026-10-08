-- P0-7 : les PDF (contrats, états des lieux, quittances) sont désormais dans des buckets MinIO privés.
-- On ne stocke plus une URL publique mais la clé de l'objet dans son bucket ; l'accès se fait
-- par une URL présignée de courte durée générée par l'API.

ALTER TABLE contrats        RENAME COLUMN url_pdf TO cle_pdf;
ALTER TABLE etats_des_lieux RENAME COLUMN url_pdf TO cle_pdf;
ALTER TABLE quittances      RENAME COLUMN url_pdf TO cle_pdf;

-- Données existantes : "http(s)://hote/bucket/<cle>" -> "<cle>" (dernier segment de l'URL)
UPDATE contrats        SET cle_pdf = regexp_replace(cle_pdf, '^.*/', '') WHERE cle_pdf LIKE '%/%';
UPDATE etats_des_lieux SET cle_pdf = regexp_replace(cle_pdf, '^.*/', '') WHERE cle_pdf LIKE '%/%';
UPDATE quittances      SET cle_pdf = regexp_replace(cle_pdf, '^.*/', '') WHERE cle_pdf LIKE '%/%';
