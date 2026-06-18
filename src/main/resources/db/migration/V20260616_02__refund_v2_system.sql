-- V20260616_02__refund_v2_system.sql
-- Complete Marketplace Refund Management System — V2
-- Uses v2_refunds to avoid conflict with legacy refunds table from V20260528_02.
-- All FK constraints are guarded with to_regclass checks for safety.

-- ─────────────────────────────────────────────────────────────────────────────
-- 0. DROP LEGACY CHILD TABLES (incompatible with V2 schema)
--    The legacy refunds system (V20260528_02) created refund_evidence with a
--    different column set (no uploaded_at, uploader_user_id, etc.).
--    We drop and recreate these tables with the new V2 schema.
--    refund_messages, refund_offers, refund_returns, refund_audit_logs are new.
-- ─────────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS refund_evidence    CASCADE;
DROP TABLE IF EXISTS refund_messages    CASCADE;
DROP TABLE IF EXISTS refund_offers      CASCADE;
DROP TABLE IF EXISTS refund_returns     CASCADE;
DROP TABLE IF EXISTS refund_audit_logs  CASCADE;


-- ─────────────────────────────────────────────────────────────────────────────
-- 1. CORE REFUND TABLE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS v2_refunds (
    id                    BIGSERIAL PRIMARY KEY,
    public_reference_id   VARCHAR(50) UNIQUE NOT NULL,

    -- Order context
    order_id              BIGINT,
    order_item_id         BIGINT,

    -- Actors
    customer_id           BIGINT,
    seller_id             BIGINT,

    -- State machine
    status                VARCHAR(60)    NOT NULL DEFAULT 'REQUEST_CREATED',
    reason                VARCHAR(60)    NOT NULL,
    resolution_type       VARCHAR(30),           -- FULL_REFUND | PARTIAL_REFUND | EXCHANGE
    requested_amount      NUMERIC(12, 2) NOT NULL,
    approved_amount       NUMERIC(12, 2),
    description           TEXT,

    -- Return workflow
    return_required       BOOLEAN        NOT NULL DEFAULT FALSE,
    refund_type           VARCHAR(30)    NOT NULL DEFAULT 'WALLET',  -- WALLET | ORIGINAL_PAYMENT

    -- Risk
    risk_score            INTEGER        NOT NULL DEFAULT 0,
    risk_level            VARCHAR(20)    NOT NULL DEFAULT 'LOW',     -- LOW | MEDIUM | HIGH

    -- Seller liability (set true when admin overrides seller rejection)
    seller_liable         BOOLEAN        NOT NULL DEFAULT FALSE,

    -- Timestamps
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    closed_at             TIMESTAMP,

    -- Indexes
    CONSTRAINT chk_v2_refunds_status CHECK (status IN (
        'REQUEST_CREATED', 'PENDING_SELLER_REVIEW',
        'SELLER_APPROVED_FULL_REFUND', 'SELLER_REJECTED',
        'PARTIAL_REFUND_OFFERED', 'PARTIAL_REFUND_ACCEPTED',
        'EXCHANGE_OFFERED', 'EXCHANGE_ACCEPTED',
        'ADMIN_REVIEW_PENDING',
        'ADMIN_APPROVED_FULL_REFUND', 'ADMIN_APPROVED_PARTIAL_REFUND',
        'ADMIN_APPROVED_EXCHANGE', 'ADMIN_REJECTED',
        'RETURN_IN_TRANSIT', 'RETURN_UNDER_REVIEW',
        'RETURN_ACCEPTED', 'RETURN_REJECTED', 'RETURN_DISPUTE_PENDING',
        'ADMIN_APPROVED_RETURN', 'ADMIN_REJECTED_RETURN',
        'REFUND_PROCESSED', 'EXCHANGE_IN_PROGRESS', 'EXCHANGE_COMPLETED',
        'CASE_CLOSED', 'CLOSED_REJECTED', 'CLOSED_PARTIAL_REFUNDED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_v2_refunds_customer     ON v2_refunds(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_v2_refunds_seller       ON v2_refunds(seller_id,   created_at DESC);
CREATE INDEX IF NOT EXISTS idx_v2_refunds_order        ON v2_refunds(order_id);
CREATE INDEX IF NOT EXISTS idx_v2_refunds_order_item   ON v2_refunds(order_item_id);
CREATE INDEX IF NOT EXISTS idx_v2_refunds_status       ON v2_refunds(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_v2_refunds_public_ref   ON v2_refunds(public_reference_id);
CREATE INDEX IF NOT EXISTS idx_v2_refunds_risk         ON v2_refunds(risk_level, status);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. EVIDENCE TABLE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refund_evidence (
    id               BIGSERIAL PRIMARY KEY,
    refund_id        BIGINT        NOT NULL,
    uploader_user_id BIGINT        NOT NULL,
    uploader_role    VARCHAR(20)   NOT NULL,  -- CUSTOMER | SELLER | ADMIN
    file_url         VARCHAR(500)  NOT NULL,
    file_type        VARCHAR(50),             -- image/jpeg, application/pdf, etc.
    description      VARCHAR(500),
    uploaded_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refund_evidence_refund ON refund_evidence(refund_id, uploaded_at);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. MESSAGES TABLE (threaded conversation)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refund_messages (
    id               BIGSERIAL PRIMARY KEY,
    refund_id        BIGINT       NOT NULL,
    sender_user_id   BIGINT       NOT NULL,
    sender_role      VARCHAR(20)  NOT NULL,   -- CUSTOMER | SELLER | ADMIN
    message          TEXT         NOT NULL,
    attachment_url   VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refund_messages_refund ON refund_messages(refund_id, created_at ASC);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. OFFERS TABLE (partial refund / exchange offers from seller or admin)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refund_offers (
    id               BIGSERIAL PRIMARY KEY,
    refund_id        BIGINT        NOT NULL,
    offer_type       VARCHAR(30)   NOT NULL,  -- PARTIAL_REFUND | EXCHANGE
    offered_amount   NUMERIC(12,2),           -- set for PARTIAL_REFUND offers
    exchange_details TEXT,                    -- set for EXCHANGE offers
    offered_by       VARCHAR(20)   NOT NULL,  -- SELLER | ADMIN
    reason           TEXT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_refund_offers_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','SUPERSEDED'))
);

CREATE INDEX IF NOT EXISTS idx_refund_offers_refund ON refund_offers(refund_id, created_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. RETURNS TABLE (return shipment tracking)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refund_returns (
    id                    BIGSERIAL PRIMARY KEY,
    refund_id             BIGINT        NOT NULL UNIQUE,  -- one return per refund
    courier               VARCHAR(100),
    tracking_number       VARCHAR(150),
    shipped_at            TIMESTAMP,
    received_at           TIMESTAMP,
    return_deadline       TIMESTAMP,
    inspection_condition  VARCHAR(60),   -- GOOD | MINOR_DAMAGE | MAJOR_DAMAGE | WRONG_ITEM | MISSING_PARTS
    inspection_notes      TEXT,
    inspected_at          TIMESTAMP,
    inspected_by_user_id  BIGINT,
    status                VARCHAR(30)   NOT NULL DEFAULT 'PENDING',

    CONSTRAINT chk_refund_returns_status CHECK (status IN (
        'PENDING', 'IN_TRANSIT', 'RECEIVED', 'ACCEPTED', 'REJECTED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_refund_returns_refund ON refund_returns(refund_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. AUDIT LOGS TABLE (immutable event trail)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refund_audit_logs (
    id               BIGSERIAL PRIMARY KEY,
    refund_id        BIGINT       NOT NULL,
    actor_user_id    BIGINT,
    actor_role       VARCHAR(20),             -- CUSTOMER | SELLER | ADMIN | SYSTEM
    ip_address       VARCHAR(60),
    action           VARCHAR(100) NOT NULL,
    previous_status  VARCHAR(60),
    new_status       VARCHAR(60),
    note             TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refund_audit_refund    ON refund_audit_logs(refund_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_refund_audit_actor     ON refund_audit_logs(actor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refund_audit_action    ON refund_audit_logs(action, created_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. FOREIGN KEY CONSTRAINTS (guarded)
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    -- v2_refunds → orders
    IF to_regclass('orders') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_v2_refunds_order' AND table_name = 'v2_refunds') THEN
            ALTER TABLE v2_refunds
                ADD CONSTRAINT fk_v2_refunds_order
                FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
        END IF;
    END IF;

    -- v2_refunds → order_items
    IF to_regclass('order_items') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_v2_refunds_order_item' AND table_name = 'v2_refunds') THEN
            ALTER TABLE v2_refunds
                ADD CONSTRAINT fk_v2_refunds_order_item
                FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE SET NULL;
        END IF;
    END IF;

    -- v2_refunds → users (customer)
    IF to_regclass('users') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_v2_refunds_customer' AND table_name = 'v2_refunds') THEN
            ALTER TABLE v2_refunds
                ADD CONSTRAINT fk_v2_refunds_customer
                FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_v2_refunds_seller' AND table_name = 'v2_refunds') THEN
            ALTER TABLE v2_refunds
                ADD CONSTRAINT fk_v2_refunds_seller
                FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL;
        END IF;
    END IF;

    -- Child tables → v2_refunds
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_refund_evidence_refund' AND table_name = 'refund_evidence') THEN
        ALTER TABLE refund_evidence
            ADD CONSTRAINT fk_refund_evidence_refund
            FOREIGN KEY (refund_id) REFERENCES v2_refunds(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_refund_messages_refund' AND table_name = 'refund_messages') THEN
        ALTER TABLE refund_messages
            ADD CONSTRAINT fk_refund_messages_refund
            FOREIGN KEY (refund_id) REFERENCES v2_refunds(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_refund_offers_refund' AND table_name = 'refund_offers') THEN
        ALTER TABLE refund_offers
            ADD CONSTRAINT fk_refund_offers_refund
            FOREIGN KEY (refund_id) REFERENCES v2_refunds(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_refund_returns_refund' AND table_name = 'refund_returns') THEN
        ALTER TABLE refund_returns
            ADD CONSTRAINT fk_refund_returns_refund
            FOREIGN KEY (refund_id) REFERENCES v2_refunds(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_refund_audit_logs_refund' AND table_name = 'refund_audit_logs') THEN
        ALTER TABLE refund_audit_logs
            ADD CONSTRAINT fk_refund_audit_logs_refund
            FOREIGN KEY (refund_id) REFERENCES v2_refunds(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. SELLER LIABILITY LEDGER
--    Tracks platform-absorbed refunds where seller is held liable (admin override).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS seller_liability_ledger (
    id         BIGSERIAL PRIMARY KEY,
    refund_id  BIGINT        NOT NULL,
    seller_id  BIGINT        NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    reason     TEXT,
    status     VARCHAR(20)   NOT NULL DEFAULT 'PENDING',   -- PENDING | SETTLED | WAIVED
    created_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    settled_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_liability_seller   ON seller_liability_ledger(seller_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_liability_refund   ON seller_liability_ledger(refund_id);
CREATE INDEX IF NOT EXISTS idx_liability_status   ON seller_liability_ledger(status);
