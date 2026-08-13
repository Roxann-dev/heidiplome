ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE app_user
    ALTER COLUMN password_hash DROP DEFAULT;