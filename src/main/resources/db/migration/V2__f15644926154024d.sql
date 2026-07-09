ALTER TABLE devices
    ADD description VARCHAR(255);

ALTER TABLE devices
    ADD image_url VARCHAR(255);

ALTER TABLE devices
    ADD secret VARCHAR(255);

DROP TABLE event_publication CASCADE;

ALTER TABLE users
    DROP COLUMN preferred_language;

ALTER TABLE devices
    DROP COLUMN secret_hash;

ALTER TABLE devices
    ALTER COLUMN last_seen DROP NOT NULL;