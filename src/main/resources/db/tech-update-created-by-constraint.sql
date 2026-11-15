-- created_by is audit metadata from the admin request context, not a required
-- users-table relationship. Existing installations may have created this FK
-- automatically, so remove it explicitly.
ALTER TABLE public.tech_updates
    DROP CONSTRAINT IF EXISTS fk_tech_updates_created_by;
