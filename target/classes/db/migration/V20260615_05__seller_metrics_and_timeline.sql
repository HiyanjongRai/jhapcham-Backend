-- Phase 8: Seller Refund Performance Metrics
-- Weekly aggregated metrics per seller for the admin performance dashboard
--
-- NOTE: Inline FKs removed — added conditionally via DO blocks below.

CREATE TABLE IF NOT EXISTS seller_refund_metrics (
    id                    BIGSERIAL     PRIMARY KEY,
    seller_id             BIGINT        NOT NULL,
    period_start          DATE          NOT NULL,
    period_end            DATE          NOT NULL,
    total_orders          INTEGER       NOT NULL DEFAULT 0,
    total_refunds         INTEGER       NOT NULL DEFAULT 0,
    approved_refunds      INTEGER       NOT NULL DEFAULT 0,
    rejected_refunds      INTEGER       NOT NULL DEFAULT 0,
    disputed_refunds      INTEGER       NOT NULL DEFAULT 0,
    -- Average hours from refund REQUESTED to seller ACCEPT/REJECT
    avg_response_hours    NUMERIC(8,2),
    -- refund_rate = total_refunds / total_orders * 100
    refund_rate           NUMERIC(5,2),
    -- rejection_rate = rejected_refunds / total_refunds * 100
    rejection_rate        NUMERIC(5,2),
    -- dispute_rate = disputed_refunds / total_refunds * 100
    dispute_rate          NUMERIC(5,2),
    -- Total value of approved refunds for this period
    approved_refund_value NUMERIC(12,2) DEFAULT 0,
    computed_at           TIMESTAMP     NOT NULL DEFAULT NOW(),

    UNIQUE (seller_id, period_start)
);

CREATE INDEX IF NOT EXISTS idx_srm_seller_period ON seller_refund_metrics(seller_id, period_start DESC);
CREATE INDEX IF NOT EXISTS idx_srm_refund_rate   ON seller_refund_metrics(refund_rate DESC);

-- FK: seller_refund_metrics.seller_id → users(id)
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_srm_seller' AND table_name = 'seller_refund_metrics')
    THEN
        ALTER TABLE seller_refund_metrics ADD CONSTRAINT fk_srm_seller
            FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Phase 9: Refund Timeline Events
-- Every state transition and key action generates a timeline event visible to the customer

CREATE TABLE IF NOT EXISTS refund_timeline_events (
    id          BIGSERIAL     PRIMARY KEY,
    refund_id   BIGINT        NOT NULL,
    -- Examples: REQUESTED, SELLER_REVIEWED, APPROVED, REJECTED, ESCALATED,
    --           RETURN_REQUESTED, RETURN_SHIPPED, RETURN_RECEIVED, INSPECTED,
    --           PAYMENT_INITIATED, PAYMENT_CONFIRMED, REFUNDED, PARTIALLY_REFUNDED
    event_type  VARCHAR(60)   NOT NULL,
    actor_role  VARCHAR(20),  -- CUSTOMER, SELLER, ADMIN, SYSTEM
    actor_name  VARCHAR(120), -- Display name for UI
    description VARCHAR(500),
    -- Optional JSON metadata (e.g., amount, tracking number, condition)
    metadata    TEXT,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_timeline_refund_time ON refund_timeline_events(refund_id, created_at);

-- FK: refund_timeline_events.refund_id → refunds(refund_id)
DO $$
BEGIN
    IF to_regclass('public.refunds') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_rte_refund' AND table_name = 'refund_timeline_events')
    THEN
        ALTER TABLE refund_timeline_events ADD CONSTRAINT fk_rte_refund
            FOREIGN KEY (refund_id) REFERENCES refunds(refund_id) ON DELETE CASCADE;
    END IF;
END $$;
