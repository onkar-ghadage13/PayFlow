ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(16) NULL,
    ADD CONSTRAINT uk_users_phone_number UNIQUE (phone_number);
