-- V20260615_06: Add exchange/replacement workflow columns to refunds table
-- Supports Phase 6 (Exchange & Replacement) and Phase 4 (Wallet Credit)
-- All ALTER TABLE statements are guarded with to_regclass checks.
-- On a fresh database, Hibernate creates the refunds table first (ddl-auto=update/create).
-- These migrations run BEFORE Hibernate DDL, so we skip safely if the table isn't there yet
-- and rely on Hibernate's ddl-auto to create those columns in that case.

DO $$
BEGIN
    IF to_regclass('refunds') IS NOT NULL THEN
        -- ── Exchange & Replacement Fields ─────────────────────────────────────────
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_tracking_number') THEN
            ALTER TABLE refunds ADD COLUMN replacement_tracking_number VARCHAR(100);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_courier') THEN
            ALTER TABLE refunds ADD COLUMN replacement_courier VARCHAR(80);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_shipped_at') THEN
            ALTER TABLE refunds ADD COLUMN replacement_shipped_at TIMESTAMP;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_delivered_at') THEN
            ALTER TABLE refunds ADD COLUMN replacement_delivered_at TIMESTAMP;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='exchange_new_order_id') THEN
            ALTER TABLE refunds ADD COLUMN exchange_new_order_id BIGINT;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='exchange_notes') THEN
            ALTER TABLE refunds ADD COLUMN exchange_notes TEXT;
        END IF;

        -- ── Wallet Credit Timestamp ───────────────────────────────────────────────
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema='public' AND table_name='refunds' AND column_name='wallet_credited_at') THEN
            ALTER TABLE refunds ADD COLUMN wallet_credited_at TIMESTAMP;
        END IF;

        -- ── FK: exchange_new_order_id → orders ────────────────────────────────────
        IF to_regclass('orders') IS NOT NULL THEN
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints
                WHERE constraint_name = 'fk_refund_exchange_order' AND table_name = 'refunds'
            ) THEN
                ALTER TABLE refunds
                    ADD CONSTRAINT fk_refund_exchange_order
                    FOREIGN KEY (exchange_new_order_id)
                    REFERENCES orders(order_id)
                    ON DELETE SET NULL;
            END IF;
        END IF;
    END IF;
END $$;

-- ── Indexes on seller_refund_metrics (table created in V05, safe here) ──────────
CREATE INDEX IF NOT EXISTS idx_srm_computed_at
    ON seller_refund_metrics (computed_at DESC);

-- ── Index on wallet_transactions (table created in V04, safe here) ────────────
DO $$
BEGIN
    IF to_regclass('wallet_transactions') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_wallet_txn_type') THEN
            CREATE INDEX idx_wallet_txn_type
                ON wallet_transactions (wallet_id, type, created_at DESC);
        END IF;
    END IF;
END $$;
