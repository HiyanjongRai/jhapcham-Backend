-- Phase 3.1: Missing performance indexes for refund system queries
-- All indexes use IF NOT EXISTS for safe re-application

-- Single-column query indexes
CREATE INDEX IF NOT EXISTS idx_refunds_customer_id  ON refunds(customer_id);
CREATE INDEX IF NOT EXISTS idx_refunds_seller_id    ON refunds(seller_id);
CREATE INDEX IF NOT EXISTS idx_refunds_created_at   ON refunds(created_at);
CREATE INDEX IF NOT EXISTS idx_refunds_updated_at   ON refunds(updated_at);
CREATE INDEX IF NOT EXISTS idx_refund_items_refund  ON refund_items(refund_id);
CREATE INDEX IF NOT EXISTS idx_refund_logs_refund   ON refund_logs(refund_id);

-- Composite indexes for common list/filter queries
CREATE INDEX IF NOT EXISTS idx_refunds_customer_status ON refunds(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_refunds_seller_status   ON refunds(seller_id, status);
CREATE INDEX IF NOT EXISTS idx_refunds_created_status  ON refunds(created_at, status);
CREATE INDEX IF NOT EXISTS idx_refunds_status_updated  ON refunds(status, updated_at);

-- Evidence table index (if it exists)
DO $$
BEGIN
    IF to_regclass('refund_evidence') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_refund_evidence_refund') THEN
            CREATE INDEX idx_refund_evidence_refund ON refund_evidence(refund_id);
        END IF;
    END IF;
END $$;

-- Phase 3.2: Missing FK constraints for referential integrity
-- Uses DO blocks so re-runs are safe

DO $$
BEGIN
    -- refund_logs → refunds
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_refund_logs_refund' AND table_name = 'refund_logs'
    ) THEN
        ALTER TABLE refund_logs
          ADD CONSTRAINT fk_refund_logs_refund
          FOREIGN KEY (refund_id) REFERENCES refunds(refund_id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    -- dispute_messages → disputes (if table exists)
    IF to_regclass('dispute_messages') IS NOT NULL AND to_regclass('disputes') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_dispute_messages_dispute'
        ) THEN
            ALTER TABLE dispute_messages
              ADD CONSTRAINT fk_dispute_messages_dispute
              FOREIGN KEY (dispute_id) REFERENCES disputes(dispute_id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

DO $$
BEGIN
    -- dispute_decisions → disputes (if table exists)
    IF to_regclass('dispute_decisions') IS NOT NULL AND to_regclass('disputes') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_dispute_decisions_dispute'
        ) THEN
            ALTER TABLE dispute_decisions
              ADD CONSTRAINT fk_dispute_decisions_dispute
              FOREIGN KEY (dispute_id) REFERENCES disputes(dispute_id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;
