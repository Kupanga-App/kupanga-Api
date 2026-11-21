-- Tableau de bord du back-office : date d'inscription des comptes, pour suivre les inscriptions mois par mois.
-- Les comptes déjà présents reçoivent la date de la migration (infrastructure neuve : aucune donnée réelle à dater).
ALTER TABLE utilisateurs ADD COLUMN date_creation TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX ix_utilisateurs_date_creation ON utilisateurs (date_creation);
