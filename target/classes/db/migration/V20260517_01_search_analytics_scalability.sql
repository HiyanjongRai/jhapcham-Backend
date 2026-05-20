CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_active_price_id
    ON products (status, price, id DESC);

CREATE INDEX IF NOT EXISTS idx_products_lower_category
    ON products (lower(category));

CREATE INDEX IF NOT EXISTS idx_products_lower_brand_trgm
    ON products USING gin (lower(brand) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_lower_name_trgm
    ON products USING gin (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_search_tsv
    ON products USING gin (
        to_tsvector(
            'simple',
            coalesce(name, '') || ' ' ||
            coalesce(brand, '') || ' ' ||
            coalesce(short_description, '') || ' ' ||
            coalesce(description, '') || ' ' ||
            coalesce(specification, '') || ' ' ||
            coalesce(features, '')
        )
    );

CREATE INDEX IF NOT EXISTS idx_product_variants_product_active
    ON product_variants (product_id, active);

CREATE INDEX IF NOT EXISTS idx_product_images_product_sort
    ON product_images (product_id, is_main DESC, sort_order ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_payments_transaction_uuid
    ON payments (transaction_uuid);

CREATE INDEX IF NOT EXISTS idx_payment_events_payment_created
    ON payment_events (payment_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_commission_status_created
    ON orders (commission_status, created_at DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_platform_daily_orders AS
SELECT date_trunc('day', created_at)::date AS order_date,
       count(*) AS order_count,
       sum(grand_total) AS gross_revenue
FROM orders
GROUP BY date_trunc('day', created_at)::date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_platform_daily_orders_date
    ON mv_platform_daily_orders (order_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_product_card_metrics AS
SELECT p.id AS product_id,
       coalesce(count(DISTINCT pv.id), 0) AS total_views,
       avg(r.rating) AS average_rating,
       count(DISTINCT r.id) AS total_reviews
FROM products p
LEFT JOIN product_views pv ON pv.product_id = p.id
LEFT JOIN reviews r ON r.product_id = p.id
GROUP BY p.id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_product_card_metrics_product
    ON mv_product_card_metrics (product_id);
