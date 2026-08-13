CREATE SEQUENCE IF NOT EXISTS pins_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE pins
(
    id         BIGINT       NOT NULL,
    pin_number INTEGER      NOT NULL,
    mode       VARCHAR(255) NOT NULL,
    label      VARCHAR(255),
    device_id  BIGINT       NOT NULL,
    CONSTRAINT pk_pins PRIMARY KEY (id)
);

ALTER TABLE pins
    ADD CONSTRAINT uq_pin_device_number UNIQUE (device_id, pin_number);

ALTER TABLE pins
    ADD CONSTRAINT FK_PINS_ON_DEVICE FOREIGN KEY (device_id) REFERENCES devices (id);