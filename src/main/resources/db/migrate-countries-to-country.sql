-- Move every application FK from public.countries to public.country.
-- IDs are mapped by ISO-2/ISO-3 code, then exact name; ambiguity aborts the transaction.
BEGIN;

CREATE TEMP TABLE country_id_migration_map ON COMMIT DROP AS
WITH referenced_ids AS (
    SELECT country_id AS legacy_id FROM public.administrative_areas WHERE country_id IS NOT NULL
    UNION SELECT country_id FROM public.cities WHERE country_id IS NOT NULL
    UNION SELECT country_id FROM public.institution_imports WHERE country_id IS NOT NULL
    UNION SELECT country_id FROM public.institutions WHERE country_id IS NOT NULL
    UNION SELECT country_id FROM public.institution_sources WHERE country_id IS NOT NULL
)
SELECT refs.legacy_id, min(master.id) AS master_id, count(DISTINCT master.id) AS match_count
FROM referenced_ids refs
LEFT JOIN public.countries legacy ON legacy.id = refs.legacy_id
LEFT JOIN public.country master
  ON upper(trim(master.code)) IN (upper(trim(legacy.iso_code)), upper(trim(legacy.iso3_code)))
  OR lower(trim(master.name)) = lower(trim(legacy.name))
GROUP BY refs.legacy_id;

DO $$
DECLARE unmapped bigint; ambiguous bigint;
BEGIN
    SELECT count(*) FILTER (WHERE match_count = 0), count(*) FILTER (WHERE match_count > 1)
    INTO unmapped, ambiguous FROM country_id_migration_map;
    IF unmapped > 0 OR ambiguous > 0 THEN
        RAISE EXCEPTION 'Country migration stopped: % referenced IDs have no target and % have multiple targets. Reconcile country masters first.', unmapped, ambiguous;
    END IF;
END $$;

CREATE TEMP TABLE country_fk_migration ON COMMIT DROP AS
SELECT con.conrelid AS relation_oid, con.conname AS constraint_name,
       pg_get_constraintdef(con.oid) AS constraint_definition,
       array_length(con.conkey, 1) AS key_count, att.attname AS source_column
FROM pg_constraint con
JOIN LATERAL unnest(con.conkey) WITH ORDINALITY keys(attnum, ord) ON true
JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = keys.attnum
WHERE con.contype = 'f' AND con.confrelid = 'public.countries'::regclass;

DO $$
DECLARE fk record;
BEGIN
    IF EXISTS (SELECT 1 FROM country_fk_migration WHERE key_count <> 1 OR source_column <> 'country_id') THEN
        RAISE EXCEPTION 'Found a foreign key to public.countries that is not a single country_id column; review it manually.';
    END IF;
    FOR fk IN SELECT DISTINCT relation_oid, constraint_name FROM country_fk_migration LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.relation_oid::regclass, fk.constraint_name);
    END LOOP;
END $$;

UPDATE public.administrative_areas child SET country_id = map.master_id FROM country_id_migration_map map WHERE child.country_id = map.legacy_id;
UPDATE public.cities child SET country_id = map.master_id FROM country_id_migration_map map WHERE child.country_id = map.legacy_id;
UPDATE public.institution_imports child SET country_id = map.master_id FROM country_id_migration_map map WHERE child.country_id = map.legacy_id;
UPDATE public.institutions child SET country_id = map.master_id FROM country_id_migration_map map WHERE child.country_id = map.legacy_id;
UPDATE public.institution_sources child SET country_id = map.master_id FROM country_id_migration_map map WHERE child.country_id = map.legacy_id;

-- Remove a redundant legacy import FK already pointing to the target table.
ALTER TABLE public.institution_imports DROP CONSTRAINT IF EXISTS fk7jxao1w5nblq6vd7uen2apbr4;

DO $$
DECLARE fk record;
BEGIN
    FOR fk IN SELECT * FROM country_fk_migration LOOP
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', fk.relation_oid::regclass,
            fk.constraint_name,
            replace(replace(fk.constraint_definition, 'REFERENCES public.countries(', 'REFERENCES public.country('),
                    'REFERENCES countries(', 'REFERENCES public.country('));
    END LOOP;
END $$;

COMMIT;
