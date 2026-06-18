-- ===================================================================
-- Migration: V20260615_08__refund_proof_required_flag.sql
-- Purpose  : ISSUE-08 FIX — Add proof_required column to refunds.
--            Tracks whether a seller submitted refund completion WITHOUT
--            uploading a proof file. When true, admin must manually obtain
--            and verify payment evidence before approving the payout.
--
-- BOOT-FIX: Wrapped in DO block with to_regclass guard.
-- On a fresh database, Flyway runs before Hibernate DDL (ddl-auto=update),
-- so the refunds table may not exist yet. If it doesn't, we skip safely —
-- Hibernate will create the column from the entity definition instead.
-- ===================================================================

DO $$
BEGIN
    IF to_regclass('refunds') IS NOT NULL THEN
        -- Add proof_required column if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name   = 'refunds'
              AND column_name  = 'proof_required'
        ) THEN
            ALTER TABLE refunds
                ADD COLUMN proof_required BOOLEAN NOT NULL DEFAULT FALSE;

            COMMENT ON COLUMN refunds.proof_required IS
                'TRUE when seller claimed completion without uploading a payment proof file. '
                'Admin must obtain evidence manually before approving this payout.';
        END IF;

        -- Partial index for admin dashboard — only indexes the minority TRUE rows
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_refunds_proof_required') THEN
            CREATE INDEX idx_refunds_proof_required
                ON refunds (proof_required)
                WHERE proof_required = TRUE;
        END IF;
    END IF;
END $$;
