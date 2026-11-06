-- =====================================================================================================
-- Données de DÉVELOPPEMENT (profil dev seulement : spring.flyway.locations de application-dev.yml).
--
-- Jouée par Flyway après toutes les migrations, au premier démarrage de l'API sur une base vide :
-- si la table utilisateurs contient déjà un compte, rien n'est inséré (base existante conservée).
-- Pour repartir de zéro : docker compose -f docker-compose-dev.yml down -v  (efface les volumes).
--
-- Contenu : ~40 comptes (mot de passe commun « Kupanga-Dev-2026 »), ~250 biens en France
-- (11 villes) et en RDC (Kinshasa, Bukavu, Goma), photos (service seed-photos du compose),
-- équipements, lieux proches, locations en cours et conversations.
-- Comptes de test et détail : GUIDE-DEMARRAGE.md §6.
--
-- À tenir à jour avec le schéma : une migration qui renomme ou rend obligatoire une colonne utilisée ici
-- casse le démarrage en dev sur une base neuve. Ce script n'est jamais joué en test ni en production.
-- =====================================================================================================

CREATE OR REPLACE FUNCTION pg_temp.un(a TEXT[]) RETURNS TEXT
    LANGUAGE sql VOLATILE AS $f$ SELECT a[1 + floor(random() * array_length(a, 1))::INT] $f$;

CREATE OR REPLACE FUNCTION pg_temp.entre(mini INT, maxi INT) RETURNS INT
    LANGUAGE sql VOLATILE AS $f$ SELECT mini + floor(random() * (maxi - mini + 1))::INT $f$;

DO $seed$
DECLARE
    mdp CONSTANT TEXT := '$2b$10$i6.a/YdZ57hsGtDnmBne0.14c6fUcZCBmNzNNvUmftRrAbyoYkgtS'; -- Kupanga-Dev-2026

    prenoms_fr TEXT[] := ARRAY['Camille','Julien','Léa','Thomas','Manon','Hugo','Chloé','Nicolas','Emma','Antoine',
        'Sarah','Maxime','Laura','Pauline','Romain','Marie','Quentin','Julie','Mathieu','Clara','Élodie','Vincent',
        'Anaïs','Yasmine','Mehdi','Fatou','Karim','Paul','Océane','Louis'];
    noms_fr TEXT[] := ARRAY['Martin','Bernard','Dubois','Durand','Lefebvre','Moreau','Laurent','Simon','Michel',
        'Garcia','David','Roux','Fournier','Morel','Girard','Mercier','Dupont','Lambert','Bonnet','Legrand',
        'Garnier','Faure','Rousseau','Blanc','Guérin','Muller','Perrin','Morin','Clément','Diallo'];
    prenoms_cd TEXT[] := ARRAY['Patrick','Grâce','Jonathan','Merveille','Christian','Divine','Fiston','Gloire',
        'Héritier','Nathalie','Junior','Ruth','Serge','Esther','Trésor','Rachel','Dieudonné','Cédric','Prisca',
        'Blaise','Sandrine','Josué','Naomie','Gédéon','Chancelle','Espoir','Amani','Furaha'];
    noms_cd TEXT[] := ARRAY['Mbuyi','Mukendi','Ilunga','Kabongo','Tshibangu','Kasongo','Lukusa','Mwamba','Ngoy',
        'Kalala','Mutombo','Banza','Kanku','Nsimba','Makiese','Lutete','Mbala','Matondo','Nzuzi','Kiala',
        'Bisimwa','Cirhuza','Mugisho','Bahati','Kahindo','Masika','Paluku','Kambale'];

    rues_fr TEXT[] := ARRAY['rue de la République','avenue Jean Jaurès','rue Victor Hugo','boulevard Gambetta',
        'rue Pasteur','rue des Lilas','rue du Moulin','place de la Liberté','rue Nationale','rue Voltaire',
        'rue Émile Zola','avenue de la Gare','rue des Écoles','rue Saint-Michel','rue de la Paix','rue Lafayette',
        'boulevard Carnot','rue du Marché','allée des Tilleuls','quai des Célestins'];
    avenues_kin TEXT[] := ARRAY['de la Justice','des Aviateurs','Tombalbaye','Colonel Mondjiba','Kasa-Vubu',
        'de la Libération','Lukusa','Haut-Congo','Pumbu','Batetela','des Cliniques','Kabasele','du Commerce',
        'Mutombo Katshi','Kianza','Bandundu'];
    avenues_kivu TEXT[] := ARRAY['Patrice Lumumba','du Lac','Kabare','de la Mission','des Volcans',
        'du Rond-Point','de la Paix','Mont Goma','des Écoles','du Marché','Industrielle','Kisangani'];
    reperes TEXT[] := ARRAY['En face de l''église catholique','À côté de la station-service','Derrière le marché',
        'Près du rond-point','Après la pharmacie','En face de l''école primaire','À 50 m de l''arrêt de bus',
        'Portail bleu, à côté du salon de coiffure','Près du terrain de football','Face à la boulangerie'];
    adjectifs TEXT[] := ARRAY['avec balcon','avec terrasse','vue dégagée','proche transports','plein sud',
        'au calme','proche commerces','avec cave','avec parking','refait à neuf'];
    equip_cd TEXT[] := ARRAY['EAU_REGIDESO','FORAGE','CITERNE','ELECTRICITE_SNEL','GROUPE_ELECTROGENE',
        'PANNEAUX_SOLAIRES','CLOTURE','GARDIENNAGE','PARKING','CLIMATISATION'];

    -- Photos déposées par le service seed-photos (seed-dev/photos) : <nom>.jpg et sa vignette <nom>-v.jpg
    p_immeuble TEXT[] := ARRAY(SELECT format('seed-immeuble-%s', lpad(n::TEXT, 2, '0')) FROM generate_series(1, 8) n);
    p_maison   TEXT[] := ARRAY(SELECT format('seed-maison-%s',   lpad(n::TEXT, 2, '0')) FROM generate_series(1, 4) n);
    p_villa    TEXT[] := ARRAY(SELECT format('seed-villa-%s',    lpad(n::TEXT, 2, '0')) FROM generate_series(1, 4) n);
    p_salon    TEXT[] := ARRAY(SELECT format('seed-salon-%s',    lpad(n::TEXT, 2, '0')) FROM generate_series(1, 8) n);
    p_chambre  TEXT[] := ARRAY(SELECT format('seed-chambre-%s',  lpad(n::TEXT, 2, '0')) FROM generate_series(1, 6) n);
    p_cuisine  TEXT[] := ARRAY(SELECT format('seed-cuisine-%s',  lpad(n::TEXT, 2, '0')) FROM generate_series(1, 6) n);

    claire BIGINT; patrick BIGINT; lucas BIGINT; grace BIGINT; ines BIGINT;
    proprios_fr BIGINT[] := '{}'; proprios_cd BIGINT[] := '{}';
    locataires_fr BIGINT[] := '{}'; locataires_cd BIGINT[] := '{}';
    biens_claire BIGINT[] := '{}'; biens_patrick BIGINT[] := '{}';

    v RECORD; z RECORD; b RECORD;
    i INT; n INT; bid BIGINT; uid BIGINT; proprio BIGINT; cid BIGINT;
    t TEXT; quartier TEXT; titre TEXT; descr TEXT; devise TEXT; prec TEXT; v_ville TEXT;
    pieces INT; surface NUMERIC; loyer NUMERIC; charges NUMERIC; meuble BOOLEAN; cree TIMESTAMP;
    lat DOUBLE PRECISION; lon DOUBLE PRECISION; photos TEXT[]; eq TEXT[]; r DOUBLE PRECISION;
BEGIN
    IF EXISTS (SELECT 1 FROM utilisateurs) THEN
        RAISE NOTICE 'Données de dev : la base contient déjà des comptes, rien n''est inséré.';
        RETURN;
    END IF;

    PERFORM setseed(0.2026); -- mêmes données à chaque création de base

    -- ───────────────────────────── Villes et zones ─────────────────────────────
    CREATE TEMP TABLE seed_villes_fr (nom TEXT, lat DOUBLE PRECISION, lon DOUBLE PRECISION, cps TEXT[],
                                      quartiers TEXT[], prix_m2 NUMERIC, poids INT) ON COMMIT DROP;
    INSERT INTO seed_villes_fr VALUES
        ('Paris', 48.8566, 2.3522, '{75011,75012,75015,75017,75018,75020}',
         ARRAY['Bastille','Montmartre','Batignolles','Nation','Belleville','Convention'], 32, 20),
        ('Lyon', 45.7640, 4.8357, '{69001,69003,69004,69006,69007}',
         ARRAY['Croix-Rousse','Presqu''île','Part-Dieu','Brotteaux','Guillotière'], 16, 14),
        ('Marseille', 43.2965, 5.3698, '{13001,13006,13007,13008}',
         ARRAY['Vieux-Port','Castellane','Endoume','Prado'], 14, 10),
        ('Bordeaux', 44.8378, -0.5792, '{33000,33300,33800}', ARRAY['Chartrons','Saint-Michel','Bastide'], 15, 9),
        ('Lille', 50.6292, 3.0573, '{59000,59800}', ARRAY['Vieux-Lille','Wazemmes','Vauban'], 15, 8),
        ('Toulouse', 43.6047, 1.4442, '{31000,31400,31500}', ARRAY['Capitole','Saint-Cyprien','Carmes'], 14, 9),
        ('Nantes', 47.2184, -1.5536, '{44000,44100}', ARRAY['Bouffay','Île de Nantes','Graslin'], 14, 7),
        ('Nice', 43.7102, 7.2620, '{06000,06300}', ARRAY['Vieux-Nice','Libération','Port'], 19, 6),
        ('Strasbourg', 48.5734, 7.7521, '{67000}', ARRAY['Petite France','Krutenau','Neudorf'], 14, 5),
        ('Montpellier', 43.6108, 3.8767, '{34000}', ARRAY['Écusson','Antigone','Beaux-Arts'], 15, 5),
        ('Rennes', 48.1173, -1.6778, '{35000}', ARRAY['Centre','Thabor','Sainte-Anne'], 14, 5);

    CREATE TEMP TABLE seed_zones_cd (ville TEXT, commune TEXT, lat DOUBLE PRECISION, lon DOUBLE PRECISION,
                                     quartiers TEXT[], base_usd INT) ON COMMIT DROP;
    INSERT INTO seed_zones_cd VALUES
        ('Kinshasa', 'Gombe', -4.3060, 15.3070, ARRAY['Golf','Haut-Commandement','Socimat','Batetela'], 1500),
        ('Kinshasa', 'Ngaliema', -4.3550, 15.2550, ARRAY['Ma Campagne','Binza Delvaux','Mont Fleury','Joli Parc'], 1100),
        ('Kinshasa', 'Limete', -4.3720, 15.3420, ARRAY['Résidentiel','Industriel','Kingabwa','Mososo'], 700),
        ('Kinshasa', 'Lingwala', -4.3200, 15.2950, ARRAY['Lokole','Voix du Peuple'], 450),
        ('Kinshasa', 'Kintambo', -4.3330, 15.2700, ARRAY['Jamaïque','Nganda','Magasin'], 550),
        ('Kinshasa', 'Bandalungwa', -4.3500, 15.2900, ARRAY['Lubudi','Makelele','Tshibangu'], 400),
        ('Kinshasa', 'Kalamu', -4.3480, 15.3150, ARRAY['Matonge','Yolo','Kauka'], 380),
        ('Kinshasa', 'Lemba', -4.4000, 15.3200, ARRAY['Righini','Salongo','Foire'], 320),
        ('Kinshasa', 'Mont-Ngafula', -4.4300, 15.2600, ARRAY['Kimwenza','Mama Yemo','Lutendele'], 600),
        ('Kinshasa', 'Masina', -4.3900, 15.3900, ARRAY['Petro-Congo','Sans Fil'], 220),
        ('Bukavu', 'Ibanda', -2.5083, 28.8608, ARRAY['Nyalukemba','Ndendere','Muhumba','Panzi'], 600),
        ('Bukavu', 'Kadutu', -2.4970, 28.8480, ARRAY['Nyakaliba','Kajangu','Mosala'], 350),
        ('Bukavu', 'Bagira', -2.4750, 28.8300, ARRAY['Nyakavogo','Lumumba'], 300),
        ('Goma', 'Goma', -1.6792, 29.2228, ARRAY['Himbi','Les Volcans','Lac Vert'], 700),
        ('Goma', 'Karisimbi', -1.6650, 29.2350, ARRAY['Katindo','Mabanga Nord','Ndosho','Majengo'], 400);

    -- ───────────────────────────── Comptes de test ─────────────────────────────
    INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
    VALUES ('Claire', 'Martin', 'claire.martin@kupanga.dev', mdp, 'ROLE_PROPRIETAIRE', TRUE, TRUE, '+33612345678')
    RETURNING id INTO claire;
    INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
    VALUES ('Patrick', 'Mbuyi', 'patrick.mbuyi@kupanga.dev', mdp, 'ROLE_PROPRIETAIRE', TRUE, TRUE, '+243812345678')
    RETURNING id INTO patrick;
    INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
    VALUES ('Lucas', 'Bernard', 'lucas.bernard@kupanga.dev', mdp, 'ROLE_LOCATAIRE', TRUE, TRUE, '+33687654321')
    RETURNING id INTO lucas;
    INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
    VALUES ('Grâce', 'Mukendi', 'grace.mukendi@kupanga.dev', mdp, 'ROLE_LOCATAIRE', TRUE, TRUE, '+243970001122')
    RETURNING id INTO grace;
    INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
    VALUES ('Inès', 'Moreau', 'ines.moreau@kupanga.dev', mdp, 'ROLE_LOCATAIRE', TRUE, TRUE, NULL)
    RETURNING id INTO ines;
    INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
    VALUES ('Hugo', 'Lefèvre', 'non.confirme@kupanga.dev', mdp, 'ROLE_LOCATAIRE', TRUE, FALSE, NULL);

    -- ─────────────────── Comptes générés : proprio.fr01…, proprio.cd01…, locataire.fr01…, locataire.cd01… ───────────────────
    FOR i IN 1..20 LOOP
        INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
        VALUES (pg_temp.un(prenoms_fr), pg_temp.un(noms_fr), format('proprio.fr%s@kupanga.dev', lpad(i::TEXT, 2, '0')),
                mdp, 'ROLE_PROPRIETAIRE', TRUE, TRUE,
                CASE WHEN random() < 0.6 THEN '+336' || pg_temp.entre(10000000, 99999999) END)
        RETURNING id INTO uid;
        proprios_fr := proprios_fr || uid;
    END LOOP;
    FOR i IN 1..15 LOOP
        INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
        VALUES (pg_temp.un(prenoms_cd), pg_temp.un(noms_cd), format('proprio.cd%s@kupanga.dev', lpad(i::TEXT, 2, '0')),
                mdp, 'ROLE_PROPRIETAIRE', TRUE, TRUE,
                CASE WHEN random() < 0.8 THEN '+2438' || pg_temp.entre(10000000, 99999999) END)
        RETURNING id INTO uid;
        proprios_cd := proprios_cd || uid;
    END LOOP;
    FOR i IN 1..15 LOOP
        INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
        VALUES (pg_temp.un(prenoms_fr), pg_temp.un(noms_fr), format('locataire.fr%s@kupanga.dev', lpad(i::TEXT, 2, '0')),
                mdp, 'ROLE_LOCATAIRE', TRUE, TRUE,
                CASE WHEN random() < 0.5 THEN '+336' || pg_temp.entre(10000000, 99999999) END)
        RETURNING id INTO uid;
        locataires_fr := locataires_fr || uid;
    END LOOP;
    FOR i IN 1..10 LOOP
        INSERT INTO utilisateurs (prenom, nom, email, mot_de_passe, role, a_completer_profil, email_verifie, telephone)
        VALUES (pg_temp.un(prenoms_cd), pg_temp.un(noms_cd), format('locataire.cd%s@kupanga.dev', lpad(i::TEXT, 2, '0')),
                mdp, 'ROLE_LOCATAIRE', TRUE, TRUE,
                CASE WHEN random() < 0.7 THEN '+2439' || pg_temp.entre(10000000, 99999999) END)
        RETURNING id INTO uid;
        locataires_cd := locataires_cd || uid;
    END LOOP;

    -- ───────────────────────────── Biens en France (140) ─────────────────────────────
    FOR i IN 1..140 LOOP
        IF i <= 6 THEN
            proprio := claire;
            SELECT * INTO v FROM seed_villes_fr WHERE seed_villes_fr.nom = CASE WHEN i <= 4 THEN 'Lyon' ELSE 'Paris' END;
        ELSE
            proprio := proprios_fr[pg_temp.entre(1, array_length(proprios_fr, 1))];
            SELECT * INTO v FROM seed_villes_fr, generate_series(1, poids) ORDER BY random() LIMIT 1;
        END IF;

        r := random();
        t := CASE WHEN r < 0.6 THEN 'APPARTEMENT' WHEN r < 0.8 THEN 'STUDIO' ELSE 'MAISON' END;
        IF t = 'STUDIO' THEN pieces := 1; surface := pg_temp.entre(16, 32);
        ELSIF t = 'APPARTEMENT' THEN pieces := pg_temp.entre(2, 5); surface := pieces * pg_temp.entre(14, 20) + pg_temp.entre(5, 20);
        ELSE pieces := pg_temp.entre(4, 7); surface := pg_temp.entre(85, 190);
        END IF;
        meuble := (t = 'STUDIO' AND random() < 0.7) OR random() < 0.15;
        quartier := pg_temp.un(v.quartiers);
        loyer := round(surface * v.prix_m2 * (0.85 + random() * 0.35) / 5) * 5;
        charges := round(surface * (1.2 + random() * 1.6) / 5) * 5;
        titre := CASE t WHEN 'APPARTEMENT' THEN 'Appartement T' || pieces WHEN 'STUDIO' THEN 'Studio'
                        ELSE 'Maison ' || pieces || ' pièces' END || ' ' || pg_temp.un(adjectifs) || ', ' || quartier;
        descr := format('Au cœur du quartier %s, %s de %s m² avec %s pièce%s. ', quartier,
                        CASE t WHEN 'MAISON' THEN 'cette maison' WHEN 'STUDIO' THEN 'ce studio' ELSE 'cet appartement' END,
                        surface, pieces, CASE WHEN pieces > 1 THEN 's' ELSE '' END)
              || pg_temp.un(ARRAY['Séjour lumineux, cuisine équipée ouverte.', 'Parquet ancien et belle hauteur sous plafond.',
                                  'Cuisine séparée équipée, nombreux rangements.', 'Double vitrage récent, chauffage individuel.'])
              || ' ' || pg_temp.un(ARRAY[format('Commerces, écoles et transports à pied dans %s.', v.nom),
                                         'Immeuble bien entretenu, digicode.', 'Copropriété calme et arborée.', 'Cave et local à vélos.'])
              || CASE WHEN meuble THEN ' Loué meublé.' ELSE ' Loué vide.' END;
        cree := now() - make_interval(days => pg_temp.entre(1, 300), hours => pg_temp.entre(0, 23));

        INSERT INTO biens (titre, adresse, ville, code_postal, pays, description, type_bien, localisation, created_at,
                           updated_at, proprietaire_id, surface_habitable, nombre_pieces, nombre_chambres, etage, ascenseur,
                           annee_construction, mode_chauffage, classe_energie, classe_ges, loyer_mensuel,
                           charges_mensuelles, depot_garantie, meuble, colocation, disponible_de, archive, devise,
                           precision_localisation)
        VALUES (titre, pg_temp.entre(1, 140) || ' ' || pg_temp.un(rues_fr), v.nom, pg_temp.un(v.cps), 'FR', descr, t,
                ST_SetSRID(ST_MakePoint(v.lon + (random() - 0.5) * 0.05, v.lat + (random() - 0.5) * 0.036), 4326),
                cree, cree, proprio, surface, pieces, greatest(pieces - 1, 0),
                CASE WHEN t = 'MAISON' THEN NULL ELSE pg_temp.entre(0, 7) END,
                t <> 'MAISON' AND random() < 0.5, pg_temp.entre(1880, 2022),
                pg_temp.un(ARRAY['ELECTRIQUE','GAZ','COLLECTIF','POMPE_A_CHALEUR']),
                pg_temp.un(ARRAY['A','B','C','C','D','D','D','E','E','F','G']),
                pg_temp.un(ARRAY['A','B','B','C','C','D','D','E','F']),
                loyer, charges, loyer * CASE WHEN meuble THEN 2 ELSE 1 END, meuble,
                pieces >= 4 AND random() < 0.2, current_date + pg_temp.entre(-30, 75), FALSE, 'EUR', 'ADRESSE')
        RETURNING id INTO bid;
        IF proprio = claire THEN biens_claire := biens_claire || bid; END IF;

        photos := ARRAY[CASE WHEN t = 'MAISON' THEN pg_temp.un(p_maison) ELSE pg_temp.un(p_immeuble) END,
                        pg_temp.un(p_salon), pg_temp.un(p_cuisine), pg_temp.un(p_chambre)];
        INSERT INTO bien_images (bien_id, cle, cle_vignette)
        SELECT bid, p || '.jpg', p || '-v.jpg' FROM unnest(photos) WITH ORDINALITY AS x(p, o) ORDER BY o;

        INSERT INTO bien_equipements (bien_id, equipement)
        SELECT bid, e FROM unnest(ARRAY['PARKING','CLIMATISATION']) e WHERE random() < 0.25;

        INSERT INTO bien_poi (bien_id, poi_type, present, rayon_metres, nombre_trouve)
        SELECT bid, p, k > 0, 5000, k
        FROM (SELECT p, CASE WHEN random() < 0.8 THEN pg_temp.entre(1, 14) ELSE 0 END AS k
              FROM unnest(ARRAY['SCHOOL','HOSPITAL','PHARMACY','KINDERGARTEN']) p) x;
    END LOOP;

    -- ─────────────────── Biens en RDC (110 : Kinshasa 70, Bukavu 20, Goma 20) ───────────────────
    FOR i IN 1..110 LOOP
        v_ville := CASE WHEN i <= 70 THEN 'Kinshasa' WHEN i <= 90 THEN 'Bukavu' ELSE 'Goma' END;
        IF i IN (1, 2, 3, 4, 71, 91) THEN proprio := patrick;
        ELSE proprio := proprios_cd[pg_temp.entre(1, array_length(proprios_cd, 1))];
        END IF;
        SELECT * INTO z FROM seed_zones_cd WHERE seed_zones_cd.ville = v_ville ORDER BY random() LIMIT 1;

        r := random();
        t := CASE WHEN r < 0.25 THEN 'APPARTEMENT' WHEN r < 0.47 THEN 'MAISON' WHEN r < 0.62 THEN 'VILLA'
                  WHEN r < 0.75 THEN 'ANNEXE' WHEN r < 0.85 THEN 'CHAMBRE' WHEN r < 0.95 THEN 'LOCAL_COMMERCIAL'
                  ELSE 'ENTREPOT' END;
        pieces := CASE t WHEN 'CHAMBRE' THEN 1 WHEN 'ANNEXE' THEN pg_temp.entre(2, 3) WHEN 'APPARTEMENT' THEN pg_temp.entre(2, 5)
                         WHEN 'MAISON' THEN pg_temp.entre(3, 6) WHEN 'VILLA' THEN pg_temp.entre(5, 9)
                         WHEN 'LOCAL_COMMERCIAL' THEN pg_temp.entre(1, 3) ELSE 1 END;
        surface := CASE t WHEN 'CHAMBRE' THEN pg_temp.entre(12, 20) WHEN 'ANNEXE' THEN pg_temp.entre(30, 60)
                          WHEN 'APPARTEMENT' THEN pg_temp.entre(55, 160) WHEN 'MAISON' THEN pg_temp.entre(90, 220)
                          WHEN 'VILLA' THEN pg_temp.entre(220, 600) WHEN 'LOCAL_COMMERCIAL' THEN pg_temp.entre(25, 150)
                          ELSE pg_temp.entre(200, 1200) END;
        loyer := greatest(40, round(z.base_usd * CASE t WHEN 'CHAMBRE' THEN 0.12 WHEN 'ANNEXE' THEN 0.35 WHEN 'APPARTEMENT' THEN 1
                     WHEN 'MAISON' THEN 1.2 WHEN 'VILLA' THEN 2.6 WHEN 'LOCAL_COMMERCIAL' THEN 1.1 ELSE 2.2 END
                     * (0.7 + random() * 0.6) / 10) * 10);
        devise := CASE WHEN t IN ('CHAMBRE','ANNEXE','APPARTEMENT','MAISON') AND random() < 0.2 THEN 'CDF' ELSE 'USD' END;
        IF devise = 'CDF' THEN loyer := round(loyer * 2800 / 5000) * 5000; END IF;
        meuble := t IN ('ANNEXE', 'CHAMBRE') AND random() < 0.4;
        quartier := pg_temp.un(z.quartiers);
        r := random();
        prec := CASE WHEN r < 0.4 THEN 'REPERE' WHEN r < 0.65 THEN 'ADRESSE' WHEN r < 0.9 THEN 'QUARTIER' ELSE 'COMMUNE' END;
        lat := z.lat + (random() - 0.5) * CASE WHEN prec IN ('REPERE','ADRESSE') THEN 0.024 ELSE 0.008 END;
        lon := z.lon + (random() - 0.5) * CASE WHEN prec IN ('REPERE','ADRESSE') THEN 0.024 ELSE 0.008 END;

        eq := ARRAY(SELECT e FROM unnest(equip_cd) e WHERE random() < 0.4);
        IF t = 'VILLA' THEN
            eq := ARRAY(SELECT DISTINCT e FROM unnest(eq || ARRAY['CLOTURE','GARDIENNAGE','FORAGE']) e);
        END IF;

        titre := CASE t WHEN 'VILLA' THEN 'Villa ' || (pieces - 2) || ' chambres' WHEN 'MAISON' THEN 'Maison ' || pieces || ' pièces'
                        WHEN 'APPARTEMENT' THEN 'Appartement ' || pieces || ' pièces' WHEN 'ANNEXE' THEN 'Annexe indépendante'
                        WHEN 'CHAMBRE' THEN 'Chambre à louer' WHEN 'LOCAL_COMMERCIAL' THEN 'Local commercial' ELSE 'Entrepôt' END
                 || ' à ' || quartier || ' (' || z.commune || ')';
        descr := CASE t WHEN 'VILLA' THEN 'Belle villa' WHEN 'MAISON' THEN 'Maison' WHEN 'APPARTEMENT' THEN 'Appartement'
                        WHEN 'ANNEXE' THEN 'Annexe indépendante' WHEN 'CHAMBRE' THEN 'Chambre' WHEN 'LOCAL_COMMERCIAL' THEN 'Local commercial'
                        ELSE 'Entrepôt' END
              || format(' à %s, %s, quartier %s.', z.ville, z.commune, quartier)
              || CASE WHEN 'FORAGE' = ANY(eq) OR 'EAU_REGIDESO' = ANY(eq) THEN ' Eau courante.' ELSE '' END
              || CASE WHEN 'GROUPE_ELECTROGENE' = ANY(eq) OR 'PANNEAUX_SOLAIRES' = ANY(eq) THEN ' Courant de secours.' ELSE '' END
              || CASE WHEN 'GARDIENNAGE' = ANY(eq) THEN ' Gardien jour et nuit.' ELSE '' END
              || ' ' || pg_temp.un(ARRAY['Accès facile, route asphaltée jusqu''au portail.', 'Quartier calme et sécurisé.',
                                         'Proche du marché et des écoles.', 'Idéal pour une famille.',
                                         'Visites sur rendez-vous avec le propriétaire.']);
        cree := now() - make_interval(days => pg_temp.entre(1, 300), hours => pg_temp.entre(0, 23));

        INSERT INTO biens (titre, adresse, ville, code_postal, pays, description, type_bien, localisation, created_at,
                           updated_at, proprietaire_id, surface_habitable, nombre_pieces, nombre_chambres, ascenseur,
                           loyer_mensuel, charges_mensuelles, meuble, colocation, disponible_de, archive, devise,
                           commune, quartier, numero_parcelle, point_de_repere, precision_localisation, garantie_mois,
                           avance_mois, heures_electricite_par_jour)
        VALUES (titre, 'n° ' || pg_temp.entre(1, 250) || ', avenue '
                       || pg_temp.un(CASE WHEN z.ville = 'Kinshasa' THEN avenues_kin ELSE avenues_kivu END),
                z.ville, NULL, 'CD', descr, t, ST_SetSRID(ST_MakePoint(lon, lat), 4326), cree, cree, proprio,
                surface, pieces, greatest(pieces - 2, 0), FALSE, loyer, 0, meuble, FALSE,
                current_date + pg_temp.entre(-20, 60), FALSE, devise, z.commune, quartier, pg_temp.entre(1, 9999)::TEXT,
                CASE WHEN random() < 0.75 THEN pg_temp.un(reperes) END, prec,
                pg_temp.un(ARRAY['1','2','3','3','6'])::INT, pg_temp.un(ARRAY['1','2','3','3','6','12'])::INT,
                pg_temp.un(ARRAY['6','8','12','16','18','20','24'])::INT)
        RETURNING id INTO bid;
        IF proprio = patrick THEN biens_patrick := biens_patrick || bid; END IF;

        photos := CASE
            WHEN t IN ('LOCAL_COMMERCIAL', 'ENTREPOT') THEN ARRAY[pg_temp.un(p_immeuble)]
            WHEN t = 'CHAMBRE' THEN ARRAY[pg_temp.un(p_chambre), pg_temp.un(p_salon)]
            WHEN t = 'VILLA' THEN ARRAY[pg_temp.un(p_villa), pg_temp.un(p_salon), pg_temp.un(p_chambre), pg_temp.un(p_cuisine)]
            WHEN t = 'APPARTEMENT' THEN ARRAY[pg_temp.un(p_immeuble), pg_temp.un(p_salon), pg_temp.un(p_chambre), pg_temp.un(p_cuisine)]
            ELSE ARRAY[pg_temp.un(p_maison), pg_temp.un(p_salon), pg_temp.un(p_chambre)] END;
        INSERT INTO bien_images (bien_id, cle, cle_vignette)
        SELECT bid, p || '.jpg', p || '-v.jpg' FROM unnest(photos) WITH ORDINALITY AS x(p, o) ORDER BY o;

        INSERT INTO bien_equipements (bien_id, equipement) SELECT bid, e FROM unnest(eq) e;

        IF prec IN ('REPERE', 'ADRESSE') THEN
            INSERT INTO bien_poi (bien_id, poi_type, present, rayon_metres, nombre_trouve)
            SELECT bid, p, k > 0, 5000, k
            FROM (SELECT p, CASE WHEN random() < 0.7 THEN pg_temp.entre(1, 10) ELSE 0 END AS k
                  FROM unnest(ARRAY['SCHOOL','HOSPITAL','PHARMACY','KINDERGARTEN']) p) x;
        END IF;
    END LOOP;

    -- ───────────────────────────── Locations en cours ─────────────────────────────
    -- Lucas loue le 1er bien de Claire (Lyon), Grâce le 1er bien de Patrick (Kinshasa)
    UPDATE biens SET locataire_id = lucas WHERE id = biens_claire[1];
    UPDATE biens SET locataire_id = grace WHERE id = biens_patrick[1];
    -- environ 12 % des autres biens loués par les locataires générés
    FOR b IN SELECT id, pays FROM biens
             WHERE proprietaire_id NOT IN (claire, patrick) AND random() < 0.12 LOOP
        UPDATE biens SET locataire_id = CASE WHEN b.pays = 'FR'
                THEN locataires_fr[pg_temp.entre(1, array_length(locataires_fr, 1))]
                ELSE locataires_cd[pg_temp.entre(1, array_length(locataires_cd, 1))] END
        WHERE id = b.id;
    END LOOP;
    INSERT INTO locations (bien_id, proprietaire_id, locataire_id, statut, date_reservation, date_expiration, date_debut)
    SELECT id, proprietaire_id, locataire_id, 'ACTIVE', d - INTERVAL '5 days', d + INTERVAL '9 days', d
    FROM (SELECT id, proprietaire_id, locataire_id,
                 created_at + make_interval(days => pg_temp.entre(5, 30)) AS d0 FROM biens WHERE locataire_id IS NOT NULL) s,
         LATERAL (SELECT least(d0, now() - INTERVAL '3 days') AS d) l;

    -- 4 biens archivés (back-office), jamais ceux des comptes de test ni les biens loués
    UPDATE biens SET archive = TRUE, date_archivage = now() - INTERVAL '10 days'
    WHERE id IN (SELECT id FROM biens WHERE locataire_id IS NULL AND proprietaire_id NOT IN (claire, patrick)
                 ORDER BY random() LIMIT 4);

    -- ───────────────────────────── Conversations ─────────────────────────────
    -- Inès écrit à Claire et à Patrick ; 30 autres premiers contacts entre locataires générés et propriétaires
    FOR b IN
        (SELECT x.id, x.proprietaire_id, ines AS candidat FROM biens x WHERE x.id IN (biens_claire[2], biens_patrick[2]))
        UNION ALL
        (SELECT x.id, x.proprietaire_id,
                CASE WHEN x.pays = 'FR' THEN locataires_fr[pg_temp.entre(1, array_length(locataires_fr, 1))]
                     ELSE locataires_cd[pg_temp.entre(1, array_length(locataires_cd, 1))] END
         FROM biens x
         WHERE x.locataire_id IS NULL AND NOT x.archive AND x.proprietaire_id NOT IN (claire, patrick)
         ORDER BY random() LIMIT 30)
    LOOP
        cree := now() - make_interval(days => pg_temp.entre(1, 20), hours => pg_temp.entre(1, 23));
        INSERT INTO conversations (bien_id, last_message, last_message_at, created_at, email_expediteur, email_destinataire)
        VALUES (b.id, 'Avec plaisir, quel créneau vous arrange ?', cree + INTERVAL '2 hours', cree,
                (SELECT email FROM utilisateurs WHERE id = b.candidat),
                (SELECT email FROM utilisateurs WHERE id = b.proprietaire_id))
        RETURNING id INTO cid;
        INSERT INTO messages (contenu, created_at, lu, expediteur_id, destinataire_id, conversation_id) VALUES
            (pg_temp.un(ARRAY['Bonjour, le logement est-il toujours disponible ?',
                              'Bonjour, je suis intéressée par votre annonce.',
                              'Serait-il possible de visiter cette semaine ?',
                              'Mbote ! Le bien est-il encore libre ?']),
             cree, TRUE, b.candidat, b.proprietaire_id, cid),
            ('Avec plaisir, quel créneau vous arrange ?', cree + INTERVAL '2 hours', random() < 0.5,
             b.proprietaire_id, b.candidat, cid);
    END LOOP;

    SELECT count(*) INTO n FROM biens;
    RAISE NOTICE 'Données de dev : % comptes, % biens insérés.', (SELECT count(*) FROM utilisateurs), n;
END
$seed$;
