-- R1 : vignette de 400 px des photos des biens (listes, cartes) ; la photo principale passe à 1600 px.
-- Les photos envoyées avant R1 n'ont pas de vignette : l'API renvoie alors la photo d'origine.
ALTER TABLE bien_images ADD COLUMN url_vignette VARCHAR(500);
