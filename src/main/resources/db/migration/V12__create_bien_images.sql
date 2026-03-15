CREATE TABLE bien_images (
    id      BIGSERIAL    PRIMARY KEY,
    url     VARCHAR(500) NOT NULL,
    bien_id BIGINT       NOT NULL REFERENCES biens(id) ON DELETE CASCADE
);