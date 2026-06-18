-- Phase 4: Wallet System
-- Provides instant wallet/store-credit refund capability as an alternative to gateway reversals

CREATE TABLE IF NOT EXISTS wallets (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    currency    VARCHAR(10)  NOT NULL DEFAULT 'NPR',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id          BIGSERIAL    PRIMARY KEY,
    wallet_id   BIGINT       NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    refund_id   BIGINT       REFERENCES refunds(refund_id) ON DELETE SET NULL,
    -- CREDIT: money added to wallet (refund credited)
    -- DEBIT: money deducted from wallet (purchase or admin adjustment)
    type        VARCHAR(10)  NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    amount      NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    balance_after NUMERIC(12,2) NOT NULL,
    description VARCHAR(500),
    created_by  VARCHAR(30),  -- SYSTEM, ADMIN, CUSTOMER
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_txn_wallet_id  ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_refund_id  ON wallet_transactions(refund_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_created_at ON wallet_transactions(wallet_id, created_at DESC);

-- Phase 6: Exchange & Replacement fields
-- Adds replacement tracking fields and exchange workflow support
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_tracking') THEN
        ALTER TABLE refunds ADD COLUMN replacement_tracking    VARCHAR(150);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_shipped_at') THEN
        ALTER TABLE refunds ADD COLUMN replacement_shipped_at  TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='refunds' AND column_name='replacement_delivered_at') THEN
        ALTER TABLE refunds ADD COLUMN replacement_delivered_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='refunds' AND column_name='exchange_sent_at') THEN
        ALTER TABLE refunds ADD COLUMN exchange_sent_at        TIMESTAMP;
    END IF;
END $$;
