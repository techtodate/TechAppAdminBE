-- Reconcile the two institution schema generations without deleting data.
-- Canonical country master: public.countries, as referenced by sources,
-- institutions and the original fk_import_country constraint.
BEGIN;
ALTER TABLE public.institution_imports DROP CONSTRAINT IF EXISTS fk7jxao1w5nblq6vd7uen2apbr4;
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
COMMIT;
