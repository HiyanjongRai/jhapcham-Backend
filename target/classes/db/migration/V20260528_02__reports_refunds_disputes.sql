-- V20260528_02__reports_refunds_disputes.sql
-- Migration file for Product Reports, Seller Reports, Customer Reports, Refunds, and Disputes

CREATE TABLE IF NOT EXISTS product_reports (
    report_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT,
    reporter_id BIGINT,
    reason VARCHAR(50) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL,
    priority INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS report_attachments (
    attachment_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT REFERENCES product_reports(report_id) ON DELETE CASCADE,
    file_url VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS seller_reports (
    report_id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT,
    reporter_id BIGINT,
    reason VARCHAR(50) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS seller_penalties (
    penalty_id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT,
    type VARCHAR(30) NOT NULL,
    description TEXT,
    issued_by BIGINT,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS seller_trust_scores (
    seller_id BIGINT PRIMARY KEY,
    score INT NOT NULL DEFAULT 100,
    fraud_risk_score INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_reports (
    report_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT,
    reporter_id BIGINT,
    order_id BIGINT,
    reason VARCHAR(50) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_flags (
    flag_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT,
    type VARCHAR(30) NOT NULL,
    description TEXT,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

-- NOTE: refunds table is managed by Hibernate (Refund.java entity).
-- This CREATE TABLE IF NOT EXISTS is a safety net only; Hibernate's ddl-auto=update
-- will create the actual table with all required columns before this runs.
CREATE TABLE IF NOT EXISTS refunds (
    refund_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    customer_id BIGINT,
    seller_id BIGINT,
    total_amount NUMERIC(10, 2) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS refund_items (
    refund_item_id BIGSERIAL PRIMARY KEY,
    refund_id      BIGINT,
    order_item_id  BIGINT,
    quantity       INT NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS refund_logs (
    log_id         BIGSERIAL PRIMARY KEY,
    refund_id      BIGINT,
    action         VARCHAR(50) NOT NULL,
    performed_by   VARCHAR(30) NOT NULL,
    comment        TEXT,
    timestamp      TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS disputes (
    dispute_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    buyer_id BIGINT,
    seller_id BIGINT,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS dispute_messages (
    message_id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT REFERENCES disputes(dispute_id) ON DELETE CASCADE,
    sender_id BIGINT,
    role VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    attachment_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS dispute_decisions (
    decision_id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT REFERENCES disputes(dispute_id) ON DELETE CASCADE,
    decided_by BIGINT,
    outcome VARCHAR(50) NOT NULL,
    notes TEXT,
    decided_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS report_moderation_logs (
    log_id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(30) NOT NULL,
    report_id BIGINT NOT NULL,
    moderator_id BIGINT,
    action VARCHAR(50) NOT NULL,
    note TEXT,
    timestamp TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF to_regclass('products') IS NOT NULL THEN
        ALTER TABLE product_reports ADD CONSTRAINT fk_product_reports_product
            FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL;
    END IF;

    IF to_regclass('users') IS NOT NULL THEN
        ALTER TABLE product_reports ADD CONSTRAINT fk_product_reports_reporter
            FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE seller_reports ADD CONSTRAINT fk_seller_reports_seller
            FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE seller_reports ADD CONSTRAINT fk_seller_reports_reporter
            FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE seller_penalties ADD CONSTRAINT fk_seller_penalties_seller
            FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE seller_penalties ADD CONSTRAINT fk_seller_penalties_issued_by
            FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE seller_trust_scores ADD CONSTRAINT fk_seller_trust_scores_seller
            FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE;
        ALTER TABLE customer_reports ADD CONSTRAINT fk_customer_reports_customer
            FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE customer_reports ADD CONSTRAINT fk_customer_reports_reporter
            FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE customer_flags ADD CONSTRAINT fk_customer_flags_customer
            FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE;
        ALTER TABLE refunds ADD CONSTRAINT fk_refunds_customer
            FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE refunds ADD CONSTRAINT fk_refunds_seller
            FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE disputes ADD CONSTRAINT fk_disputes_buyer
            FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE disputes ADD CONSTRAINT fk_disputes_seller
            FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE dispute_messages ADD CONSTRAINT fk_dispute_messages_sender
            FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE dispute_decisions ADD CONSTRAINT fk_dispute_decisions_decided_by
            FOREIGN KEY (decided_by) REFERENCES users(id) ON DELETE SET NULL;
        ALTER TABLE report_moderation_logs ADD CONSTRAINT fk_report_moderation_logs_moderator
            FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF to_regclass('orders') IS NOT NULL THEN
        ALTER TABLE customer_reports ADD CONSTRAINT fk_customer_reports_order
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
        ALTER TABLE refunds ADD CONSTRAINT fk_refunds_order
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
        ALTER TABLE disputes ADD CONSTRAINT fk_disputes_order
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
    END IF;

    IF to_regclass('order_items') IS NOT NULL THEN
        ALTER TABLE refund_items ADD CONSTRAINT fk_refund_items_order_item
            FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Indexes for search optimization and fast queues sorting
CREATE INDEX idx_product_reports_status ON product_reports(status);
CREATE INDEX idx_product_reports_prod_status ON product_reports(product_id, status);
CREATE INDEX idx_product_reports_queue ON product_reports(status, priority, updated_at);
CREATE INDEX idx_seller_reports_seller ON seller_reports(seller_id);
CREATE INDEX idx_seller_reports_queue ON seller_reports(status, updated_at);
CREATE INDEX idx_customer_reports_cust ON customer_reports(customer_id);
CREATE INDEX idx_customer_reports_queue ON customer_reports(status, updated_at);
CREATE INDEX idx_refunds_order ON refunds(order_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_queue ON refunds(status, updated_at);
CREATE INDEX idx_refund_items_order_item ON refund_items(order_item_id);
CREATE INDEX idx_disputes_order ON disputes(order_id);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_queue ON disputes(status, updated_at);
CREATE INDEX idx_dispute_messages_thread ON dispute_messages(dispute_id, created_at);
CREATE INDEX idx_report_logs_report ON report_moderation_logs(report_type, report_id);
