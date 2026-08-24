ALTER TABLE schedule
DROP
CONSTRAINT fk_schedule_on_device;

ALTER TABLE schedule
    ADD device_key VARCHAR(255);

ALTER TABLE schedule
    ALTER COLUMN device_key SET NOT NULL;

ALTER TABLE schedule
    ADD CONSTRAINT uc_95721642d6345c031f1dac5a1 UNIQUE (day_of_week);

ALTER TABLE schedule
DROP
COLUMN device_id;