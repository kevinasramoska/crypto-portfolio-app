-- A collision intentionally aborts migration. Operators must resolve ownership, never delete accounts automatically.
UPDATE users SET email = LOWER(TRIM(email));
ALTER TABLE users ADD CONSTRAINT ck_users_normalized_email CHECK (email = LOWER(TRIM(email)));
