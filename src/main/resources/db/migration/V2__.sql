CREATE TABLE schedule_sync
(
    device_key VARCHAR(255) NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT pk_schedule_sync PRIMARY KEY (device_key)
);

ALTER TABLE schedule
    ADD CONSTRAINT uc_f5de195ddbbd09ba7c9f7ef2c UNIQUE (device_key, date);
