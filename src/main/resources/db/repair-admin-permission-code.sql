/*
 * Repairs public.admin_permissions when authentication reports that p.code is
 * missing. In the original Admin schema, permission names are permission codes
 * (for example USER_VIEW), so this backfills code without changing role links.
 * Safe to rerun. Prefer admin-access-control.sql for the complete Admin schema.
 */
BEGIN;

ALTER TABLE public.admin_permissions
    ADD COLUMN IF NOT EXISTS code VARCHAR(100);

DO $$
DECLARE
    label_column TEXT;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='admin_permissions' AND column_name='name'
    ) THEN
        label_column := 'name';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='admin_permissions' AND column_name='permission_name'
    ) THEN
        label_column := 'permission_name';
    ELSE
        RAISE EXCEPTION 'Cannot derive admin_permissions.code: expected a name or permission_name column';
    END IF;

    EXECUTE format($sql$
        WITH labels AS (
            SELECT id,
                   trim(both '_' FROM left(upper(regexp_replace(
                       COALESCE(NULLIF(btrim(%1$I::text), ''), 'PERMISSION_' || id::text),
                       '[^A-Za-z0-9]+', '_', 'g'
                   )), 100)) AS base_code
            FROM public.admin_permissions
        ), ranked AS (
            SELECT id, base_code,
                   row_number() OVER (PARTITION BY base_code ORDER BY id) AS duplicate_number
            FROM labels
        )
        UPDATE public.admin_permissions p
        SET code = CASE
            WHEN ranked.duplicate_number = 1 AND NOT EXISTS (
                SELECT 1 FROM public.admin_permissions existing
                WHERE existing.id <> ranked.id AND existing.code = ranked.base_code
            ) THEN ranked.base_code
            ELSE left(ranked.base_code, greatest(1, 99 - length(ranked.id::text))) || '_' || ranked.id::text
        END
        FROM ranked
        WHERE p.id = ranked.id AND (p.code IS NULL OR btrim(p.code) = '')
    $sql$, label_column);
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.admin_permissions WHERE code IS NULL OR btrim(code) = '') THEN
        RAISE EXCEPTION 'Some Admin permissions still have no code; review their names before continuing';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_permissions_code
    ON public.admin_permissions(code);

ALTER TABLE public.admin_permissions
    ALTER COLUMN code SET NOT NULL;

COMMIT;
