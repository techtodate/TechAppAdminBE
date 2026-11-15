/*
 * One-time upgrade for databases that applied the retired Admin verification
 * schema. Repoint review-table foreign keys to the Main Application's canonical
 * request table. This does not copy or synchronize request data.
 *
 * The migration fails atomically if existing review rows do not have matching
 * canonical request IDs; resolve those orphaned rows before retrying.
 */
BEGIN;

DO $$
DECLARE
    fk RECORD;
    child_table TEXT;
BEGIN
    IF to_regclass('public.user_verification_requests') IS NULL THEN
        RAISE EXCEPTION 'Canonical table public.user_verification_requests does not exist';
    END IF;

    IF to_regclass('public.verification_requests') IS NOT NULL THEN
        FOR fk IN
            SELECT c.conname, c.conrelid::regclass AS child_relation
            FROM pg_constraint c
            WHERE c.contype = 'f'
              AND c.confrelid = to_regclass('public.verification_requests')
              AND c.conrelid IN (
                  SELECT t.oid
                  FROM pg_class t
                  JOIN pg_namespace n ON n.oid = t.relnamespace
                  WHERE n.nspname = 'public'
                    AND t.relname IN ('verification_sections','verification_evidence','verification_identity','verification_review_history')
              )
        LOOP
            child_table := fk.child_relation::text;
            EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', child_table, fk.conname);
            EXECUTE format(
                'ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (verification_request_id) REFERENCES public.user_verification_requests(id) ON DELETE CASCADE NOT VALID',
                child_table, fk.conname
            );
            EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', child_table, fk.conname);
        END LOOP;
    END IF;
END $$;

COMMIT;
