ALTER TABLE IF EXISTS tokens
    ADD CONSTRAINT fk_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE IF EXISTS devices
    ADD CONSTRAINT fk_devices_user
        FOREIGN KEY (user_id) REFERENCES users (id);