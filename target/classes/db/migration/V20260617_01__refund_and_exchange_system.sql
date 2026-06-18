-- Drop legacy tables to avoid conflicts
DROP TABLE IF EXISTS refund_evidence CASCADE;
DROP TABLE IF EXISTS refund_messages CASCADE;
DROP TABLE IF EXISTS refund_offers CASCADE;
DROP TABLE IF EXISTS refund_returns CASCADE;
DROP TABLE IF EXISTS refund_audit_logs CASCADE;
DROP TABLE IF EXISTS refund_audit_log CASCADE;
DROP TABLE IF EXISTS refund_inspection CASCADE;
DROP TABLE IF EXISTS refunds CASCADE;
DROP TABLE IF EXISTS v2_refunds CASCADE;

-- 1. Create refunds table
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    refund_number VARCHAR(50) UNIQUE NOT NULL,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    customer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    seller_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL, -- REFUND, EXCHANGE, PARTIAL_REFUND
    status VARCHAR(60) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT,
    verdict VARCHAR(100), -- VALID_DAMAGE, FRAUDULENT_CLAIM, DAMAGED_BY_CUSTOMER, PARTIAL_DAMAGE
    refund_amount NUMERIC(12, 2),
    damage_score INTEGER, -- 1 to 10
    inspection_notes TEXT,
    tracking_number VARCHAR(150),
    admin_decision VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Create refund_evidence table
CREATE TABLE refund_evidence (
    id BIGSERIAL PRIMARY KEY,
    refund_id BIGINT NOT NULL REFERENCES refunds(id) ON DELETE CASCADE,
    file_url VARCHAR(500) NOT NULL,
    note VARCHAR(500),
    uploaded_by VARCHAR(100) NOT NULL, -- customer email or role info
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Create refund_inspection table (7 checks + severity + verdict)
CREATE TABLE refund_inspection (
    id BIGSERIAL PRIMARY KEY,
    refund_id BIGINT NOT NULL REFERENCES refunds(id) ON DELETE CASCADE,
    physical_damage BOOLEAN NOT NULL DEFAULT FALSE,
    water_damage BOOLEAN NOT NULL DEFAULT FALSE,
    missing_parts BOOLEAN NOT NULL DEFAULT FALSE,
    burn_damage BOOLEAN NOT NULL DEFAULT FALSE,
    tampering BOOLEAN NOT NULL DEFAULT FALSE,
    packaging_intact BOOLEAN NOT NULL DEFAULT FALSE,
    product_matches BOOLEAN NOT NULL DEFAULT FALSE,
    severity_score INTEGER NOT NULL, -- 1 to 10
    inspector_notes TEXT,
    verdict VARCHAR(100) NOT NULL, -- VALID_DAMAGE, FRAUDULENT_CLAIM, DAMAGED_BY_CUSTOMER, PARTIAL_DAMAGE
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 4. Create refund_audit_log table
CREATE TABLE refund_audit_log (
    id BIGSERIAL PRIMARY KEY,
    refund_id BIGINT NOT NULL REFERENCES refunds(id) ON DELETE CASCADE,
    old_status VARCHAR(60),
    new_status VARCHAR(60) NOT NULL,
    actor_id BIGINT,
    actor_role VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_refunds_customer ON refunds(customer_id);
CREATE INDEX idx_refunds_seller ON refunds(seller_id);
CREATE INDEX idx_refunds_order ON refunds(order_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refund_evidence_refund ON refund_evidence(refund_id);
CREATE INDEX idx_refund_inspection_refund ON refund_inspection(refund_id);
CREATE INDEX idx_refund_audit_log_refund ON refund_audit_log(refund_id);
