CREATE TABLE IF NOT EXISTS refund_requests (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(80) NOT NULL,
    reason_details TEXT,
    status VARCHAR(40) NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(120),
    item_subtotal NUMERIC(38, 2) NOT NULL DEFAULT 0,
    tax_refund NUMERIC(38, 2) NOT NULL DEFAULT 0,
    shipping_refund NUMERIC(38, 2) NOT NULL DEFAULT 0,
    discount_adjustment NUMERIC(38, 2) NOT NULL DEFAULT 0,
    total_refund NUMERIC(38, 2) NOT NULL DEFAULT 0,
    seller_commission_reversal NUMERIC(38, 2) NOT NULL DEFAULT 0,
    shipping_included BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_score INTEGER NOT NULL DEFAULT 0,
    fraud_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    customer_notes TEXT,
    seller_notes TEXT,
    admin_notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    approved_at TIMESTAMP,
    refunded_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT uk_refund_customer_idempotency UNIQUE (customer_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS refund_line_items (
    id BIGSERIAL PRIMARY KEY,
    refund_request_id BIGINT NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    order_item_id BIGINT NOT NULL REFERENCES order_items(id),
    product_id_snapshot BIGINT NOT NULL,
    product_name_snapshot VARCHAR(1000) NOT NULL,
    product_image_snapshot VARCHAR(500),
    quantity_requested INTEGER NOT NULL,
    unit_price_snapshot NUMERIC(38, 2) NOT NULL,
    item_subtotal NUMERIC(38, 2) NOT NULL DEFAULT 0,
    tax_refund NUMERIC(38, 2) NOT NULL DEFAULT 0,
    discount_adjustment NUMERIC(38, 2) NOT NULL DEFAULT 0,
    total_refund NUMERIC(38, 2) NOT NULL DEFAULT 0,
    seller_commission_reversal NUMERIC(38, 2) NOT NULL DEFAULT 0,
    restock_inventory BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_refund_line_item_order_item UNIQUE (order_item_id)
);

CREATE TABLE IF NOT EXISTS refund_status_history (
    id BIGSERIAL PRIMARY KEY,
    refund_request_id BIGINT NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    actor_id BIGINT REFERENCES users(id),
    actor_type VARCHAR(40) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refund_evidence (
    id BIGSERIAL PRIMARY KEY,
    refund_request_id BIGINT NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    uploaded_by_user_id BIGINT NOT NULL REFERENCES users(id),
    file_path VARCHAR(700) NOT NULL,
    file_type VARCHAR(120),
    file_size BIGINT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refund_transactions (
    id BIGSERIAL PRIMARY KEY,
    refund_request_id BIGINT NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    gateway VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    amount NUMERIC(38, 2) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    provider_payment_reference VARCHAR(255),
    provider_refund_reference VARCHAR(255),
    request_payload TEXT,
    response_payload TEXT,
    failure_reason TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    CONSTRAINT uk_refund_transaction_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS refund_fraud_signals (
    id BIGSERIAL PRIMARY KEY,
    refund_request_id BIGINT NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    signal_type VARCHAR(120) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    score INTEGER NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refund_requests_customer_created ON refund_requests(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refund_requests_seller_status ON refund_requests(seller_id, status);
CREATE INDEX IF NOT EXISTS idx_refund_requests_status_created ON refund_requests(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refund_requests_order ON refund_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_refund_line_items_request ON refund_line_items(refund_request_id);
CREATE INDEX IF NOT EXISTS idx_refund_line_items_order_item ON refund_line_items(order_item_id);
CREATE INDEX IF NOT EXISTS idx_refund_status_history_request_created ON refund_status_history(refund_request_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refund_evidence_request ON refund_evidence(refund_request_id);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_request ON refund_transactions(refund_request_id);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_gateway_status ON refund_transactions(gateway, status);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_provider_reference ON refund_transactions(provider_refund_reference);
CREATE INDEX IF NOT EXISTS idx_refund_fraud_request ON refund_fraud_signals(refund_request_id);
CREATE INDEX IF NOT EXISTS idx_refund_fraud_user_created ON refund_fraud_signals(user_id, created_at DESC);
