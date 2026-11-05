-- Optional owner e-mail captured during self-service owner registration; may also be used to sign in.
ALTER TABLE users ADD COLUMN email VARCHAR(120) NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
