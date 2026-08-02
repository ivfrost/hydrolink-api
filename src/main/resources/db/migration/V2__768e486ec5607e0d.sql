ALTER TABLE devices
    ADD location_coordinates VARCHAR(255);

ALTER TABLE devices
    ADD location_label VARCHAR(255);

DROP TABLE IF EXISTS event_publication CASCADE;

ALTER TABLE devices DROP COLUMN IF EXISTS last_seen;
ALTER TABLE devices DROP COLUMN IF EXISTS location;

ALTER TABLE devices
    ALTER COLUMN created_at SET DEFAULT NOW();

ALTER TABLE users
    ALTER COLUMN created_at SET DEFAULT NOW();

ALTER TABLE devices
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE users
    ALTER COLUMN updated_at SET DEFAULT NOW();