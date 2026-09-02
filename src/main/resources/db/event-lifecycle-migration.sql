BEGIN;

ALTER TABLE public.events
    DROP CONSTRAINT IF EXISTS chk_events_status;

ALTER TABLE public.events
    ALTER COLUMN status TYPE character varying(20);

ALTER TABLE public.events
    ADD CONSTRAINT chk_events_status CHECK (
        status IN (
            'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'REJECTED', 'APPROVED',
            'PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'LIVE',
            'COMPLETED', 'CANCELLED', 'POSTPONED'
        )
    );

UPDATE public.events
SET status_changed_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE status_changed_at IS NULL;

ALTER TABLE public.events
    ALTER COLUMN status_changed_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN status_changed_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_events_status_submitted_at
    ON public.events (status, submitted_at, id);

COMMIT;
