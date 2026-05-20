CREATE TABLE IF NOT EXISTS reports (
    id bigserial PRIMARY KEY,
    custom_report_id varchar(100) UNIQUE,
    order_id bigint NOT NULL REFERENCES orders(id),
    order_item_id bigint NOT NULL REFERENCES order_items(id),
    reported_entity_id bigint NOT NULL,
    reported_entity_name varchar(255),
    reported_entity_image varchar(500),
    type varchar(50) NOT NULL,
    customer_id bigint NOT NULL REFERENCES users(id),
    reporter_id bigint NOT NULL REFERENCES users(id),
    seller_id bigint NOT NULL REFERENCES users(id),
    reason varchar(100) NOT NULL,
    description text NOT NULL,
    status varchar(50) NOT NULL,
    seller_comment text,
    admin_comment text,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    seller_action_at timestamp,
    admin_action_at timestamp
);

CREATE TABLE IF NOT EXISTS report_evidence (
    report_id bigint NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    evidence_url varchar(500) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_order ON reports(order_id);
CREATE INDEX IF NOT EXISTS idx_reports_order_item ON reports(order_item_id);
CREATE INDEX IF NOT EXISTS idx_reports_customer ON reports(customer_id);
CREATE INDEX IF NOT EXISTS idx_reports_seller ON reports(seller_id);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
