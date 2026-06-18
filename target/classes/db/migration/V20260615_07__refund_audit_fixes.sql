-- V20260615_07__refund_audit_fixes.sql
-- Fixes from the Jhapcham Refund System Audit Report (2026-06-15)

-- ─────────────────────────────────────────────────────────────────────────────
-- DB-03 FIX: Add status column to seller_liability_ledger
-- Previously the ledger was fully append-only with no state machine,
-- making it impossible to mark a liability as settled or disputed.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'seller_liability_ledger') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'seller_liability_ledger' AND column_name = 'status'
        ) THEN
            ALTER TABLE seller_liability_ledger
                ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

            COMMENT ON COLUMN seller_liability_ledger.status IS
                'Lifecycle state of the liability record. '
                'PENDING = recorded but not yet settled, '
                'SETTLED = admin has confirmed seller paid, '
                'WAIVED = admin waived the liability, '
                'DISPUTED = seller disputed the charge.';
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'seller_liability_ledger' AND column_name = 'collected_at'
        ) THEN
            ALTER TABLE seller_liability_ledger
                ADD COLUMN collected_at TIMESTAMP,
                ADD COLUMN waived_at TIMESTAMP,
                ADD COLUMN waive_reason TEXT,
                ADD COLUMN admin_note TEXT;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_seller_liability_status
    ON seller_liability_ledger (seller_id, status);

-- ─────────────────────────────────────────────────────────────────────────────
-- DB-04 FIX: Add FK constraint on wallet_transactions.refund_id
-- Previously orphaned wallet transactions could exist if a refund was deleted.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'wallet_transactions')
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'wallet_transactions' AND column_name = 'refund_id')
    THEN
        -- Only add FK if refunds table exists and constraint does not already exist
        IF to_regclass('refunds') IS NOT NULL
           AND NOT EXISTS (
               SELECT 1 FROM information_schema.table_constraints tc
               JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
               WHERE tc.table_name = 'wallet_transactions'
               AND tc.constraint_name = 'fk_wallet_tx_refund'
           )
        THEN
            -- Use ON DELETE SET NULL so wallet history is preserved even if a refund record is removed
            ALTER TABLE wallet_transactions
                ADD CONSTRAINT fk_wallet_tx_refund
                FOREIGN KEY (refund_id) REFERENCES refunds(refund_id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- DB-01 FIX: Widen refunds.reason column from VARCHAR(50) to VARCHAR(255)
-- Long reason codes (e.g. custom admin notes) were silently truncated / threw DB errors.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refunds' AND column_name = 'reason'
        AND character_maximum_length IS NOT NULL AND character_maximum_length <= 50
    ) THEN
        ALTER TABLE refunds ALTER COLUMN reason TYPE VARCHAR(255);
    END IF;
END $$;
