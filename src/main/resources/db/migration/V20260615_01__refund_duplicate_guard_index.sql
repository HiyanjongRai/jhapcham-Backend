-- Phase 1.1: DB-level unique partial index to prevent duplicate active refunds per order
-- Prevents race-condition double submissions even when the application-level check is bypassed
DO $$
BEGIN
    -- Unique partial index: only one non-terminal refund per order can exist at the DB level
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_refunds_one_active_per_order') THEN
        CREATE UNIQUE INDEX idx_refunds_one_active_per_order
          ON refunds(order_id)
          WHERE status NOT IN (
            'REJECTED','CANCELLED','FAILED',
            'REFUNDED','PARTIALLY_REFUNDED','COMPLETED'
          );
    END IF;
END $$;
