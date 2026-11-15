-- Reconcile the two institution schema generations without deleting data.
-- Canonical country master: public.country, as referenced by sources,
-- institutions and the original fk_import_country constraint.
BEGIN;
ALTER TABLE public.institution_imports DROP CONSTRAINT IF EXISTS fk7jxao1w5nblq6vd7uen2apbr4;
-- Add columns introduced by the current JPA model. The deployed database may
-- still have the previous import schema (file_hash/completed_at/error_message).
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS file_checksum varchar(64);
UPDATE public.institution_imports SET file_checksum = left(file_hash, 64)
WHERE file_checksum IS NULL AND file_hash IS NOT NULL;
UPDATE public.institution_imports SET file_checksum = 'legacy-import-' || id::text
WHERE file_checksum IS NULL;
ALTER TABLE public.institution_imports ALTER COLUMN file_checksum SET NOT NULL;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS update_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS skipped_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS failure_reason text;
UPDATE public.institution_imports SET failure_reason = error_message
WHERE failure_reason IS NULL AND error_message IS NOT NULL;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS applied_at timestamp without time zone;
UPDATE public.institution_imports SET applied_at = completed_at
WHERE applied_at IS NULL AND completed_at IS NOT NULL;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS update_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS updated_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS inserted_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS total_records integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS new_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS unchanged_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS duplicate_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS error_count integer NOT NULL DEFAULT 0;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS created_by bigint;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS file_name varchar(255);
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS file_path varchar(1000);
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS source_id bigint;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS country_id bigint;
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS country_code varchar(3);
ALTER TABLE public.institution_imports ADD COLUMN IF NOT EXISTS ignore_duplicate_check boolean NOT NULL DEFAULT false;
-- Preserve legacy state references while exposing the canonical area column.
ALTER TABLE public.institutions ADD COLUMN IF NOT EXISTS administrative_area_id bigint;
UPDATE public.institutions SET administrative_area_id = state_id
WHERE administrative_area_id IS NULL AND state_id IS NOT NULL;
ALTER TABLE public.cities ADD COLUMN IF NOT EXISTS administrative_area_id bigint;
-- Legacy aliases are retained for existing readers, but are not required for
-- writes using row_number/action/processing_status/institution_id.
ALTER TABLE public.institution_import_records ALTER COLUMN row_index DROP NOT NULL;
ALTER TABLE public.institution_import_records ALTER COLUMN validation_status DROP NOT NULL;
ALTER TABLE public.institution_import_records ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.institution_import_record_matches ALTER COLUMN matched_institution_id DROP NOT NULL;
ALTER TABLE public.institution_import_record_matches ALTER COLUMN match_confidence DROP NOT NULL;
ALTER TABLE public.institution_import_record_matches ALTER COLUMN resolution_status DROP NOT NULL;
ALTER TABLE public.institution_import_record_matches ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.institution_aliases ALTER COLUMN normalized_alias DROP NOT NULL;
ALTER TABLE public.institution_aliases ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.institution_source_records ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.institution_source_records ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
-- Match the API/entity length contracts; widening preserves existing values.
ALTER TABLE public.institution_imports ALTER COLUMN version TYPE varchar(100);
ALTER TABLE public.institution_imports ALTER COLUMN file_path TYPE varchar(1000);
ALTER TABLE public.institution_source_mappings ALTER COLUMN source_field_name TYPE varchar(200);
ALTER TABLE public.institution_source_mappings ALTER COLUMN transformation_rule TYPE varchar(100);
ALTER TABLE public.institution_import_records ALTER COLUMN source_identifier TYPE varchar(250);
ALTER TABLE public.institution_source_records ALTER COLUMN source_identifier TYPE varchar(250);
ALTER TABLE public.institution_aliases ALTER COLUMN alias_name TYPE varchar(500);
ALTER TABLE public.institution_standard_fields ALTER COLUMN description TYPE varchar(500);
ALTER TABLE public.institutions ALTER COLUMN name TYPE varchar(500);
ALTER TABLE public.institutions ALTER COLUMN normalized_name TYPE varchar(500);
ALTER TABLE public.institutions ALTER COLUMN short_name TYPE varchar(250);
ALTER TABLE public.institutions ALTER COLUMN website TYPE varchar(1000);
ALTER TABLE public.institutions ALTER COLUMN normalized_website TYPE varchar(1000);
ALTER TABLE public.institutions ALTER COLUMN address TYPE text;
-- Administrative area is imported as source text and can exceed the legacy 10-character code width.
ALTER TABLE public.institutions ALTER COLUMN administrative_area TYPE varchar(200);
COMMIT;
