-- Consolidate the legacy trainer_payment_accounts table into the existing
-- trainer_payout_accounts model.
--
-- Based on the supplied preflight: trainer_payment_accounts is empty and its
-- only FK dependency is training_settlements.payment_account_id.
-- Run after trainer-payout-account-setup-v1.sql has been applied.
-- This script is transactional and aborts if the inspected schema/data changed.

BEGIN;

LOCK TABLE public.training_settlements,
           public.trainer_payout_accounts,
           public.trainer_payment_accounts
    IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    legacy_rows BIGINT;
    unexpected_fk BIGINT;
BEGIN
    IF to_regclass('public.training_settlements') IS NULL
       OR to_regclass('public.trainer_payout_accounts') IS NULL
       OR to_regclass('public.trainer_payment_accounts') IS NULL THEN
        RAISE EXCEPTION 'Expected settlement and payout account tables are missing';
    END IF;

    SELECT COUNT(*) INTO legacy_rows FROM public.trainer_payment_accounts;
    IF legacy_rows <> 0 THEN
        RAISE EXCEPTION 'trainer_payment_accounts now contains % rows; migrate/reconcile those rows before dropping the legacy table', legacy_rows;
    END IF;

    SELECT COUNT(*) INTO unexpected_fk
    FROM pg_constraint c
    WHERE c.contype='f'
      AND c.confrelid='public.trainer_payment_accounts'::regclass
      AND NOT (
          c.conrelid='public.training_settlements'::regclass
          AND c.conname='training_settlements_payment_account_id_fkey'
      );
    IF unexpected_fk <> 0 THEN
        RAISE EXCEPTION 'Unexpected foreign keys reference trainer_payment_accounts; inspect dependencies before migration';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        WHERE c.contype='f'
          AND c.conrelid='public.training_settlements'::regclass
          AND c.conname='training_settlements_payment_account_id_fkey'
          AND c.confrelid='public.trainer_payment_accounts'::regclass
    ) THEN
        RAISE EXCEPTION 'Expected legacy settlement account foreign key was not found';
    END IF;
END $$;

-- Remove the legacy FK before assigning payout-account IDs. Otherwise the
-- UPDATE below is checked against trainer_payment_accounts and fails even
-- when the assigned ID exists in trainer_payout_accounts.
ALTER TABLE public.training_settlements
    DROP CONSTRAINT training_settlements_payment_account_id_fkey;

-- Attach settlements with no account reference to the trainer's primary
-- payout account for the same payment provider. This covers IDs 2 and 3 from
-- the supplied preflight results. Account verification remains governed by
-- trainer_payout_accounts.account_status and trainer_profiles payout status.
UPDATE public.training_settlements s
SET payment_account_id = payout.id
FROM public.payment_providers provider,
     public.trainer_payout_accounts payout
WHERE s.payment_account_id IS NULL
  AND provider.id=s.payment_provider_id
  AND payout.trainer_user_id=s.trainer_user_id
  AND payout.provider=provider.code
  AND payout.is_primary=TRUE;

DO $$
DECLARE
    orphaned_settlements BIGINT;
BEGIN
    SELECT COUNT(*) INTO orphaned_settlements
    FROM public.training_settlements s
    WHERE s.payment_account_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM public.trainer_payout_accounts payout
          WHERE payout.id=s.payment_account_id
      );

    IF orphaned_settlements <> 0 THEN
        RAISE EXCEPTION '% settlement rows still reference account IDs absent from trainer_payout_accounts; reconcile those rows before retrying', orphaned_settlements;
    END IF;
END $$;

ALTER TABLE public.training_settlements
    ADD CONSTRAINT training_settlements_payment_account_id_fkey
    FOREIGN KEY (payment_account_id)
    REFERENCES public.trainer_payout_accounts(id);

-- RESTRICT makes PostgreSQL refuse the drop if a dependency was missed.
DROP TABLE public.trainer_payment_accounts RESTRICT;

COMMIT;
