CREATE TABLE notifications
(
    id               BIGSERIAL    PRIMARY KEY,
    destinataire_id  BIGINT       NOT NULL REFERENCES utilisateurs (id),
    type             VARCHAR(50)  NOT NULL,
    titre            VARCHAR(255) NOT NULL,
    message          TEXT         NOT NULL,
    lue              BOOLEAN      NOT NULL DEFAULT FALSE,
    lien             VARCHAR(500),
    reference_id     BIGINT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_destinataire_lue ON notifications (destinataire_id, lue);
