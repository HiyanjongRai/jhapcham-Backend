-- Phase 4: Wallet System
-- Provides instant wallet/store-credit refund capability as an alternative to gateway reversals
--
-- NOTE: Inline FKs removed from CREATE TABLE — added conditionally via DO blocks below
-- because users/refunds tables may be created by Hibernate AFTER Flyway runs.

CREATE TABLE IF NOT EXISTS wallets (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL UNIQUE,
    balance     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    currency    VARCHAR(10)   NOT NULL DEFAULT 'NPR',
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id            BIGSERIAL     PRIMARY KEY,
    wallet_id     BIGINT        NOT NULL,
    refund_id     BIGINT,
    -- CREDIT: money added to wallet (refund credited)
    -- DEBIT: money deducted from wallet (purchase or admin adjustment)
    type          VARCHAR(10)   NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    amount        NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    balance_after NUMERIC(12,2) NOT NULL,
    description   VARCHAR(500),
    created_by    VARCHAR(30),  -- SYSTEM, ADMIN, CUSTOMER
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_txn_wallet_id  ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_refund_id  ON wallet_transactions(refund_id);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_created_at ON wallet_transactions(wallet_id, created_at DESC);

-- FK: wallets.user_id → users(id)
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_wallet_user' AND table_name = 'wallets')
    THEN
        ALTER TABLE wallets ADD CONSTRAINT fk_wallet_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- FK: wallet_transactions.wallet_id → wallets(id)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_wallet_txn_wallet' AND table_name = 'wallet_transactions')
    THEN
        ALTER TABLE wallet_transactions ADD CONSTRAINT fk_wallet_txn_wallet
            FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE;
    END IF;
END $$;

-- FK: wallet_transactions.refund_id → refunds(refund_id)
DO $$
BEGIN
    IF to_regclass('public.refunds') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_wallet_txn_refund' AND table_name = 'wallet_transactions')
    THEN
        ALTER TABLE wallet_transactions ADD CONSTRAINT fk_wallet_txn_refund
            FOREIGN KEY (refund_id) REFERENCES refunds(refund_id) ON DELETE SET NULL;
    END IF;
END $$;

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
