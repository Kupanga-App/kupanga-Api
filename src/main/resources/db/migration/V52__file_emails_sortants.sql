-- M2 (Sprint 5) : file d'envoi persistante des e-mails (« transactional outbox »).
-- Un e-mail est écrit ici dans la même transaction que l'action qui le demande (inscription, signature, quittance…),
-- puis envoyé par l'application avec de nouveaux essais en cas de panne : il n'est jamais perdu, et jamais envoyé
-- pour une action annulée.

CREATE TABLE emails_sortants (
    id              BIGSERIAL                PRIMARY KEY,
    cree_le         TIMESTAMP WITH TIME ZONE NOT NULL,
    type            VARCHAR(40)              NOT NULL,
    priorite        VARCHAR(10)              NOT NULL,
    destinataire    VARCHAR(320)             NOT NULL,
    sujet           VARCHAR(500),
    -- Effacés dès l'envoi (liens de connexion, de reset, de signature), et après 30 jours sinon
    html            TEXT,
    texte           TEXT,
    statut          VARCHAR(20)              NOT NULL,
    tentatives      INTEGER                  NOT NULL DEFAULT 0,
    prochain_essai  TIMESTAMP WITH TIME ZONE NOT NULL,
    verrou_jusqua   TIMESTAMP WITH TIME ZONE,
    -- Classe de l'erreur et code SMTP seulement (ni adresse, ni sujet, ni contenu)
    derniere_erreur VARCHAR(200),
    expire_le       TIMESTAMP WITH TIME ZONE,
    envoye_le       TIMESTAMP WITH TIME ZONE,
    contenu_efface  BOOLEAN                  NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_emails_sortants_statut
        CHECK (statut IN ('A_ENVOYER', 'EN_COURS', 'ENVOYE', 'ECHEC_DEFINITIF', 'PERIME')),
    CONSTRAINT ck_emails_sortants_priorite CHECK (priorite IN ('CRITIQUE', 'NORMALE')),
    CONSTRAINT ck_emails_sortants_tentatives CHECK (tentatives >= 0)
);

-- Lecture des e-mails dus par l'envoyeur (toutes les 30 s)
CREATE INDEX ix_emails_sortants_a_envoyer
    ON emails_sortants (prochain_essai)
    WHERE statut IN ('A_ENVOYER', 'EN_COURS');

-- Purge RGPD et back-office (par date et statut)
CREATE INDEX ix_emails_sortants_statut_cree_le ON emails_sortants (statut, cree_le);

-- PDF joints : seulement leur clé dans un bucket privé, lus au moment de l'envoi
CREATE TABLE emails_sortants_pieces_jointes (
    email_id    BIGINT       NOT NULL REFERENCES emails_sortants (id) ON DELETE CASCADE,
    ordre       INTEGER      NOT NULL,
    bucket      VARCHAR(100) NOT NULL,
    cle         VARCHAR(255) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    PRIMARY KEY (email_id, ordre)
);
