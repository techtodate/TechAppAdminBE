/* Admin-side review metadata for attachments owned by the Main Application.
   Run after public.user_verification_evidence and Admin auth tables exist. */
BEGIN;

DO $$
BEGIN
    IF to_regclass('public.user_verification_requests') IS NULL THEN
        RAISE EXCEPTION 'Required table public.user_verification_requests does not exist';
    END IF;
    IF to_regclass('public.user_verification_evidence') IS NULL THEN
        RAISE EXCEPTION 'Required table public.user_verification_evidence does not exist';
    END IF;
    IF to_regclass('public.admin_users') IS NULL THEN
        RAISE EXCEPTION 'Required table public.admin_users does not exist';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.verification_evidence_reviews (
    id BIGSERIAL PRIMARY KEY,
    verification_request_id BIGINT NOT NULL
        REFERENCES public.user_verification_requests(id) ON DELETE CASCADE,
    source_evidence_id BIGINT NOT NULL
        REFERENCES public.user_verification_evidence(id) ON DELETE CASCADE,
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    review_notes TEXT,
    reviewed_by BIGINT REFERENCES public.admin_users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.verification_evidence_reviews
    DROP CONSTRAINT IF EXISTS chk_verification_evidence_reviews_status;
ALTER TABLE public.verification_evidence_reviews
    ADD CONSTRAINT chk_verification_evidence_reviews_status
    CHECK (review_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUIRED'));
CREATE UNIQUE INDEX IF NOT EXISTS uk_verification_evidence_review_source
    ON public.verification_evidence_reviews(source_evidence_id);
CREATE INDEX IF NOT EXISTS idx_verification_evidence_reviews_request
    ON public.verification_evidence_reviews(verification_request_id);
CREATE INDEX IF NOT EXISTS idx_verification_evidence_reviews_status
    ON public.verification_evidence_reviews(review_status);

/* This backend connects as the datasource role configured in
   application.properties. It needs row reads/updates and sequence access for inserts. */
GRANT SELECT, INSERT, UPDATE ON TABLE public.verification_evidence_reviews TO musthafa;
GRANT USAGE, SELECT ON SEQUENCE public.verification_evidence_reviews_id_seq TO musthafa;

/* New audit entries reference Main App attachment IDs. Old history entries
   remain intact when their IDs only existed in the retired Admin evidence table. */
DO $$
DECLARE fk_record RECORD;
BEGIN
    IF to_regclass('public.verification_review_history') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='verification_review_history' AND column_name='evidence_id') THEN
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
                FOREIGN KEY (evidence_id) REFERENCES public.user_verification_evidence(id)
                ON DELETE SET NULL NOT VALID;
        END IF;
    END IF;
END $$;

COMMIT;
