/*
 * One-time, manually executed initial Admin account creation.
 * Change the username/email placeholders and replace the password hash with a
 * BCrypt hash generated outside source control before running this script.
 * Never put the plaintext password or a real password hash in this file.
 * Run only after admin-access-control.sql has been applied.
 * Compatible with psql. Re-running for the same account does not overwrite its
 * password or create a duplicate account/role mapping.
 */
\set admin_username 'CHANGE_ME_superadmin'
\set admin_email 'CHANGE_ME_admin@zeto9.com'
\set admin_first_name 'Super'
\set admin_last_name 'Admin'
\set admin_password_hash 'REPLACE_WITH_BCRYPT_HASH'

BEGIN;

CREATE TEMPORARY TABLE initial_super_admin_input (
    username TEXT NOT NULL,
    email TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    password_hash TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO initial_super_admin_input(username,email,first_name,last_name,password_hash)
VALUES (:'admin_username', :'admin_email', :'admin_first_name', :'admin_last_name', :'admin_password_hash');

DO $$
DECLARE
    v_input initial_super_admin_input%ROWTYPE;
    v_user_id BIGINT;
    v_existing_super_admin_id BIGINT;
    v_role_id BIGINT;
BEGIN
    SELECT * INTO STRICT v_input FROM initial_super_admin_input;

    IF v_input.username = 'CHANGE_ME_superadmin'
       OR v_input.email = 'CHANGE_ME_admin@zeto9.com'
       OR v_input.password_hash = 'REPLACE_WITH_BCRYPT_HASH' THEN
        RAISE EXCEPTION 'Edit create_initial_super_admin.sql placeholders and provide a BCrypt hash before execution';
    END IF;
    IF length(v_input.username) < 3 OR length(v_input.username) > 100
       OR length(v_input.email) > 255
       OR length(v_input.password_hash) <> 60
       OR substring(v_input.password_hash FROM 1 FOR 4) NOT IN ('$2a$', '$2b$', '$2y$') THEN
        RAISE EXCEPTION 'Username/email is invalid or password_hash is not a BCrypt hash';
    END IF;

    SELECT id INTO v_role_id FROM public.admin_roles
    WHERE code = 'SUPER_ADMIN' AND is_active = TRUE;
    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Active SUPER_ADMIN role is missing; apply admin-access-control.sql first';
    END IF;

    SELECT id INTO v_user_id FROM public.admin_users
    WHERE lower(username) = lower(v_input.username) OR lower(email) = lower(v_input.email)
    FOR UPDATE;

    IF v_user_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM public.admin_users
            WHERE id = v_user_id
              AND lower(username) = lower(v_input.username)
              AND lower(email) = lower(v_input.email)
              AND is_active = TRUE
        ) THEN
            RAISE EXCEPTION 'An Admin account already uses this username or email; resolve it manually';
        END IF;
    ELSE
        INSERT INTO public.admin_users
            (username,email,password_hash,first_name,last_name,is_active,must_change_password)
        VALUES
            (lower(v_input.username),lower(v_input.email),v_input.password_hash,
             v_input.first_name,v_input.last_name,TRUE,TRUE)
        RETURNING id INTO v_user_id;
    END IF;

    SELECT u.id INTO v_existing_super_admin_id
    FROM public.admin_users u
    JOIN public.admin_user_role_mapping m ON m.admin_user_id = u.id
    JOIN public.admin_roles r ON r.id = m.admin_role_id
    WHERE u.is_active = TRUE AND r.is_active = TRUE AND r.code = 'SUPER_ADMIN'
      AND u.id <> v_user_id
    LIMIT 1;
    IF v_existing_super_admin_id IS NOT NULL THEN
        RAISE EXCEPTION 'An active SUPER_ADMIN already exists (id=%); refusing to create another initial account', v_existing_super_admin_id;
    END IF;

    INSERT INTO public.admin_user_role_mapping(admin_user_id,admin_role_id)
    VALUES (v_user_id,v_role_id)
    ON CONFLICT (admin_user_id,admin_role_id) DO NOTHING;
END $$;

COMMIT;
