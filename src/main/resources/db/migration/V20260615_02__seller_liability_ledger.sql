-- Phase 1.2: Seller Liability Ledger
-- Records seller financial liability when admin overrides a seller's rejection of a refund.
-- The platform pays the customer, but the seller's netIncome is deducted and a liability entry is created.
--
-- NOTE: FK constraints for refund_id → refunds and seller_id → users are added
-- conditionally via DO blocks below, because these tables may be created by
-- Hibernate (ddl-auto=update) AFTER Flyway runs on a fresh database.

CREATE TABLE IF NOT EXISTS seller_liability_ledger (
    id              BIGSERIAL PRIMARY KEY,
    refund_id       BIGINT        NOT NULL,
    seller_id       BIGINT        NOT NULL,
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    reason          VARCHAR(500),
    -- PENDING: liability created, not yet collected
    -- COLLECTED: admin has confirmed seller paid/deducted
    -- WAIVED: admin waived the liability (platform absorbed the cost)
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','COLLECTED','WAIVED')),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    collected_at    TIMESTAMP,
    waived_at       TIMESTAMP,
    waive_reason    VARCHAR(500),
    admin_note      VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_seller_liability_seller_status
  ON seller_liability_ledger(seller_id, status);

CREATE INDEX IF NOT EXISTS idx_seller_liability_refund
  ON seller_liability_ledger(refund_id);

-- Add FK to refunds(refund_id) — only if the refunds table exists
DO $$
BEGIN
    IF to_regclass('public.refunds') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM information_schema.table_constraints
           WHERE constraint_name = 'fk_sell_liab_refund'
             AND table_name = 'seller_liability_ledger'
       )
    THEN
        ALTER TABLE seller_liability_ledger
            ADD CONSTRAINT fk_sell_liab_refund
            FOREIGN KEY (refund_id) REFERENCES refunds(refund_id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add FK to users(id) — only if the users table exists
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM information_schema.table_constraints
           WHERE constraint_name = 'fk_sell_liab_user'
             AND table_name = 'seller_liability_ledger'
       )
    THEN
        ALTER TABLE seller_liability_ledger
            ADD CONSTRAINT fk_sell_liab_user
            FOREIGN KEY (seller_id) REFERENCES users(id);
    END IF;
END $$;

-- Phase 1.2: Add seller_liable flag to refunds table
-- Set to true when admin overrides a seller rejection — triggers liability deduction
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='refunds' AND column_name='seller_liable') THEN
        ALTER TABLE refunds ADD COLUMN seller_liable BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='refunds' AND column_name='refund_type') THEN
        ALTER TABLE refunds ADD COLUMN refund_type VARCHAR(30) NOT NULL DEFAULT 'REFUND';
    END IF;
END $$;
