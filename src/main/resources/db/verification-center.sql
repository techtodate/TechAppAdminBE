/* Supporting review data for the canonical public.user_verification_requests table.
   This migration never creates, copies, or synchronizes verification requests. */
BEGIN;

ALTER TABLE IF EXISTS public.user_verification_requests
    ADD COLUMN IF NOT EXISTS assigned_reviewer_id BIGINT,
    ADD COLUMN IF NOT EXISTS changes_requested_at TIMESTAMP WITHOUT TIME ZONE;

DO $$
DECLARE c RECORD;
BEGIN
    IF to_regclass('public.user_verification_requests') IS NULL THEN
        RAISE EXCEPTION 'Canonical public.user_verification_requests table must exist before verification-center.sql is applied';
    END IF;
    FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='public.user_verification_requests'::regclass AND contype='c'
        AND (pg_get_constraintdef(oid) ILIKE '%verification_type%' OR pg_get_constraintdef(oid) ILIKE '%status%')
    LOOP EXECUTE format('ALTER TABLE public.user_verification_requests DROP CONSTRAINT %I',c.conname); END LOOP;
END $$;
UPDATE public.user_verification_requests SET verification_type='INDIVIDUAL' WHERE verification_type='PROFESSIONAL';
ALTER TABLE public.user_verification_requests ADD CONSTRAINT chk_user_verification_type
    CHECK (verification_type IN ('INDIVIDUAL','INSTITUTION','ORGANIZATION'));
ALTER TABLE public.user_verification_requests ADD CONSTRAINT chk_user_verification_status
    CHECK (status IN ('NOT_STARTED','IN_PROGRESS','SUBMITTED','UNDER_REVIEW','CHANGES_REQUIRED','VERIFIED','REJECTED','EXPIRED','REVOKED'));

CREATE TABLE IF NOT EXISTS public.verification_sections (
    id BIGSERIAL PRIMARY KEY,
    verification_request_id BIGINT NOT NULL REFERENCES public.user_verification_requests(id) ON DELETE CASCADE,
    section_code VARCHAR(50) NOT NULL CHECK (section_code IN ('PERSONAL','EDUCATION','PROFESSIONAL','WORK_EXPERIENCE','TRAINER','IDENTITY','EVIDENCE')),
    user_status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED' CHECK (user_status IN ('NOT_STARTED','COMPLETED','CHANGES_REQUIRED')),
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED','NOT_REQUIRED')),
    review_notes TEXT, reviewed_by BIGINT REFERENCES public.admin_users(id) ON DELETE SET NULL, reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (verification_request_id, section_code)
);
/* Refresh checks on older installs too; CREATE TABLE IF NOT EXISTS does not
   update constraints when a section code or status is added later. */
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid='public.verification_sections'::regclass
          AND contype='c'
          AND (pg_get_constraintdef(oid) ILIKE '%section_code%'
            OR pg_get_constraintdef(oid) ILIKE '%user_status%'
            OR pg_get_constraintdef(oid) ILIKE '%review_status%')
    LOOP
        EXECUTE format('ALTER TABLE public.verification_sections DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;
ALTER TABLE public.verification_sections
    ADD CONSTRAINT chk_verification_section_code
        CHECK (section_code IN ('PERSONAL','EDUCATION','PROFESSIONAL','WORK_EXPERIENCE','TRAINER','IDENTITY','EVIDENCE')),
    ADD CONSTRAINT chk_verification_section_user_status
        CHECK (user_status IN ('NOT_STARTED','COMPLETED','CHANGES_REQUIRED')),
    ADD CONSTRAINT chk_verification_section_review_status
        CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED','NOT_REQUIRED'));
CREATE TABLE IF NOT EXISTS public.verification_evidence (
    id BIGSERIAL PRIMARY KEY,
    verification_request_id BIGINT NOT NULL REFERENCES public.user_verification_requests(id) ON DELETE CASCADE,
    section_code VARCHAR(50) NOT NULL, user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    document_name VARCHAR(255) NOT NULL, document_type VARCHAR(100), storage_provider VARCHAR(50) DEFAULT 'AZURE_BLOB',
    storage_container VARCHAR(255), storage_key VARCHAR(1000) NOT NULL, content_type VARCHAR(150), file_size BIGINT,
    upload_status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED')),
    review_notes TEXT, reviewed_by BIGINT REFERENCES public.admin_users(id) ON DELETE SET NULL, reviewed_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS public.verification_identity (
    id BIGSERIAL PRIMARY KEY,
    verification_request_id BIGINT NOT NULL UNIQUE REFERENCES public.user_verification_requests(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    provider VARCHAR(100), provider_reference VARCHAR(255), document_type VARCHAR(100),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING','PROCESSING','VERIFIED','FAILED')),
    name_match BOOLEAN, document_valid BOOLEAN, liveness_verified BOOLEAN, date_of_birth_match BOOLEAN, verified_name VARCHAR(255), verified_at TIMESTAMP,
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED')),
    reviewed_by BIGINT REFERENCES public.admin_users(id) ON DELETE SET NULL, reviewed_at TIMESTAMP, review_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS public.verification_review_history (
    id BIGSERIAL PRIMARY KEY,
    verification_request_id BIGINT NOT NULL REFERENCES public.user_verification_requests(id) ON DELETE CASCADE,
    admin_user_id BIGINT NOT NULL REFERENCES public.admin_users(id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL, section_code VARCHAR(50), evidence_id BIGINT REFERENCES public.user_verification_evidence(id) ON DELETE SET NULL,
    previous_status VARCHAR(30), new_status VARCHAR(30), comments TEXT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE public.verification_review_history ADD COLUMN IF NOT EXISTS admin_user_id BIGINT;

CREATE TABLE IF NOT EXISTS public.verification_evidence_reviews (
    id BIGSERIAL PRIMARY KEY,
    verification_request_id BIGINT NOT NULL REFERENCES public.user_verification_requests(id) ON DELETE CASCADE,
    source_evidence_id BIGINT NOT NULL UNIQUE REFERENCES public.user_verification_evidence(id) ON DELETE CASCADE,
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    review_notes TEXT,
    reviewed_by BIGINT REFERENCES public.admin_users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE public.verification_evidence_reviews DROP CONSTRAINT IF EXISTS chk_verification_evidence_reviews_status;
ALTER TABLE public.verification_evidence_reviews ADD CONSTRAINT chk_verification_evidence_reviews_status
    CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED'));

/* Existing history rows may reference the retired Admin evidence table. Keep
   those rows as legacy data, but send all new evidence history to Main App files. */
DO $$
DECLARE fk_record RECORD;
BEGIN
    FOR fk_record IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
        WHERE c.conrelid='public.verification_review_history'::regclass
          AND c.contype='f' AND a.attname='evidence_id'
          AND c.confrelid <> to_regclass('public.user_verification_evidence')
    LOOP
        EXECUTE format('ALTER TABLE public.verification_review_history DROP CONSTRAINT %I', fk_record.conname);
    END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
        WHERE c.conrelid='public.verification_review_history'::regclass
          AND c.contype='f' AND a.attname='evidence_id'
          AND c.confrelid=to_regclass('public.user_verification_evidence')
    ) THEN
        ALTER TABLE public.verification_review_history
            ADD CONSTRAINT fk_verification_history_source_evidence
            FOREIGN KEY (evidence_id) REFERENCES public.user_verification_evidence(id) ON DELETE SET NULL NOT VALID;
    END IF;
END $$;

/* Admin reviewer foreign keys must target admin_users. NOT VALID keeps old
   historical actor IDs readable while enforcing all new writes. */
DO $$
DECLARE fk_record RECORD; t TEXT; col TEXT; target REGCLASS;
BEGIN
    FOR t,col IN SELECT * FROM (VALUES
        ('user_verification_requests','assigned_reviewer_id'),
        ('verification_sections','reviewed_by'),
        ('verification_evidence','reviewed_by'),
        ('verification_evidence_reviews','reviewed_by'),
        ('verification_identity','reviewed_by'),
        ('verification_review_history','admin_user_id')) x(table_name,column_name)
    LOOP
        target:=to_regclass('public.'||t);
        IF target IS NULL THEN CONTINUE; END IF;
        FOR fk_record IN SELECT DISTINCT fk.conname FROM pg_constraint fk
            JOIN pg_attribute fa ON fa.attrelid=fk.conrelid AND fa.attnum=ANY(fk.conkey)
            WHERE fk.conrelid=target AND fk.contype='f' AND fa.attname=col
              AND fk.confrelid <> to_regclass('public.admin_users')
        LOOP EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',target,fk_record.conname); END LOOP;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint fc
            JOIN pg_attribute fa ON fa.attrelid=fc.conrelid AND fa.attnum=ANY(fc.conkey)
            WHERE fc.conrelid=target AND fc.contype='f' AND fa.attname=col
              AND fc.confrelid=to_regclass('public.admin_users')
        ) THEN
            IF col='admin_user_id' THEN
                EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.admin_users(id) ON DELETE RESTRICT NOT VALID',target,'fk_'||t||'_admin_actor',col);
            ELSE
                EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.admin_users(id) ON DELETE SET NULL NOT VALID',target,'fk_'||t||'_admin_reviewer',col);
            END IF;
        END IF;
    END LOOP;
END $$;
CREATE INDEX IF NOT EXISTS idx_user_verification_requests_reviewer ON public.user_verification_requests(assigned_reviewer_id);
CREATE INDEX IF NOT EXISTS idx_user_verification_requests_status_submitted ON public.user_verification_requests(status,submitted_at DESC);
/* All Admin review children must reference the canonical Main App request table,
   including installs with a stale FK target other than the former Admin table. */
DO $$
DECLARE
    child_name TEXT;
    child_table REGCLASS;
    fk RECORD;
BEGIN
    IF to_regclass('public.user_verification_requests') IS NULL THEN
        RAISE EXCEPTION 'Canonical public.user_verification_requests table does not exist';
    END IF;

    FOREACH child_name IN ARRAY ARRAY['verification_sections','verification_evidence','verification_identity','verification_review_history','verification_evidence_reviews'] LOOP
        child_table := to_regclass('public.' || child_name);
        IF child_table IS NULL THEN CONTINUE; END IF;

        FOR fk IN
            SELECT DISTINCT c.conname
            FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
            WHERE c.conrelid=child_table AND c.contype='f'
              AND a.attname='verification_request_id'
              AND c.confrelid <> to_regclass('public.user_verification_requests')
        LOOP
            EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', child_table, fk.conname);
        END LOOP;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
            WHERE c.conrelid=child_table AND c.contype='f'
              AND a.attname='verification_request_id'
              AND c.confrelid=to_regclass('public.user_verification_requests')
        ) THEN
            EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (verification_request_id) REFERENCES public.user_verification_requests(id) ON DELETE CASCADE NOT VALID',
                    child_table, 'fk_' || child_name || '_canonical_request');
            EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', child_table, 'fk_' || child_name || '_canonical_request');
        END IF;
    END LOOP;
END $$;
ALTER TABLE public.verification_evidence DROP CONSTRAINT IF EXISTS verification_evidence_review_status_check;
ALTER TABLE public.verification_evidence ADD CONSTRAINT verification_evidence_review_status_check
    CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED'));
CREATE INDEX IF NOT EXISTS idx_verification_sections_request ON public.verification_sections(verification_request_id);
CREATE INDEX IF NOT EXISTS idx_verification_sections_review_status ON public.verification_sections(review_status);
CREATE INDEX IF NOT EXISTS idx_verification_evidence_request ON public.verification_evidence(verification_request_id);
CREATE INDEX IF NOT EXISTS idx_verification_evidence_review ON public.verification_evidence(review_status);
CREATE INDEX IF NOT EXISTS idx_verification_evidence_reviews_request ON public.verification_evidence_reviews(verification_request_id);
CREATE INDEX IF NOT EXISTS idx_verification_evidence_reviews_status ON public.verification_evidence_reviews(review_status);
GRANT SELECT, INSERT, UPDATE ON TABLE public.verification_evidence_reviews TO musthafa;
GRANT USAGE, SELECT ON SEQUENCE public.verification_evidence_reviews_id_seq TO musthafa;
CREATE INDEX IF NOT EXISTS idx_verification_identity_user ON public.verification_identity(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_history_request ON public.verification_review_history(verification_request_id);
CREATE INDEX IF NOT EXISTS idx_verification_history_admin ON public.verification_review_history(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_verification_history_created ON public.verification_review_history(created_at);

COMMIT;
