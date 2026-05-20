CREATE TABLE IF NOT EXISTS product_listing_reports (
    id bigserial PRIMARY KEY,
    custom_report_id varchar(100) UNIQUE,
    product_id bigint NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    reporter_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason varchar(100) NOT NULL,
    description text NOT NULL,
    status varchar(50) NOT NULL,
    admin_comment text,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_plr_product ON product_listing_reports(product_id);
CREATE INDEX IF NOT EXISTS idx_plr_reporter ON product_listing_reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_plr_status ON product_listing_reports(status);
