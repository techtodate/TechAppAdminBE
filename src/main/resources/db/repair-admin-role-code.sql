/*
 * Repairs public.admin_roles when the application reports that r.code is
 * missing. Safe to rerun. Run with the same database/schema used by the app.
 * Prefer applying admin-access-control.sql for the complete Admin schema.
 */
BEGIN;

ALTER TABLE public.admin_roles
    ADD COLUMN IF NOT EXISTS code VARCHAR(60);

DO $$
DECLARE
    label_column TEXT;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='admin_roles' AND column_name='name'
    ) THEN
        label_column := 'name';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='admin_roles' AND column_name='role_name'
    ) THEN
        label_column := 'role_name';
    ELSE
        RAISE EXCEPTION 'Cannot derive admin_roles.code: expected a name or role_name column';
    END IF;

    EXECUTE format($sql$
        WITH labels AS (
            SELECT id,
                   trim(both '_' FROM left(upper(regexp_replace(
                       COALESCE(NULLIF(btrim(%1$I::text), ''), 'ROLE_' || id::text),
                       '[^A-Za-z0-9]+', '_', 'g'
                   )), 60)) AS base_code
            FROM public.admin_roles
        ), ranked AS (
            SELECT id, base_code,
                   row_number() OVER (PARTITION BY base_code ORDER BY id) AS duplicate_number
            FROM labels
        )
        UPDATE public.admin_roles r
        SET code = CASE
            WHEN ranked.duplicate_number = 1 AND NOT EXISTS (
                SELECT 1 FROM public.admin_roles existing
                WHERE existing.id <> ranked.id AND existing.code = ranked.base_code
            ) THEN ranked.base_code
            ELSE left(ranked.base_code, greatest(1, 59 - length(ranked.id::text))) || '_' || ranked.id::text
        END
        FROM ranked
        WHERE r.id = ranked.id AND (r.code IS NULL OR btrim(r.code) = '')
    $sql$, label_column);
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.admin_roles WHERE code IS NULL OR btrim(code) = '') THEN
        RAISE EXCEPTION 'Some admin roles still have no code; review their names before continuing';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_roles_code
    ON public.admin_roles(code);

ALTER TABLE public.admin_roles
    ALTER COLUMN code SET NOT NULL;

COMMIT;
