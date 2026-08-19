CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);

DROP TABLE event_publication CASCADE;

ALTER TABLE ota_updates
ALTER
COLUMN object_key TYPE VARCHAR(255) USING (object_key::VARCHAR(255));

ALTER TABLE devices
ALTER
COLUMN secret TYPE VARCHAR(32) USING (secret::VARCHAR(32));

ALTER TABLE ota_updates
ALTER
COLUMN sha256 TYPE VARCHAR(255) USING (sha256::VARCHAR(255));

ALTER TABLE ota_updates
ALTER
COLUMN technical_name TYPE VARCHAR(255) USING (technical_name::VARCHAR(255));