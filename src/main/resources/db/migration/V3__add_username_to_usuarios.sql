-- Existing users receive a deterministic technical username based only on the primary key.
-- This avoids deriving personal data from names or emails and is collision-free for unique IDs.
ALTER TABLE usuarios ADD COLUMN username VARCHAR(30);

UPDATE usuarios
SET username = 'usuario_' || CAST(id AS VARCHAR)
WHERE username IS NULL;

ALTER TABLE usuarios
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE usuarios
    ADD CONSTRAINT uk_usuarios_username UNIQUE (username);

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_username_formato CHECK (
        username = lower(username)
        AND char_length(username) BETWEEN 3 AND 30
        AND position(' ' in username) = 0
        AND trim(translate(username, 'abcdefghijklmnopqrstuvwxyz0123456789._-', repeat(' ', 39))) = ''
        AND trim(translate(substring(username FROM 1 FOR 1), 'abcdefghijklmnopqrstuvwxyz0123456789', repeat(' ', 36))) = ''
        AND trim(translate(substring(username FROM char_length(username) FOR 1), 'abcdefghijklmnopqrstuvwxyz0123456789', repeat(' ', 36))) = ''
    );
