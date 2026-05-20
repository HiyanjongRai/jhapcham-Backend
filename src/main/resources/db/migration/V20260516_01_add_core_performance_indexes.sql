CREATE INDEX IF NOT EXISTS idx_orders_user_created
    ON orders (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_created
    ON orders (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_payment_status_created
    ON orders (payment_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_email_created
    ON orders (customer_email, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_items_order
    ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_product
    ON order_items (product_id);

CREATE INDEX IF NOT EXISTS idx_order_items_variant
    ON order_items (variant_id);

CREATE INDEX IF NOT EXISTS idx_products_status_id
    ON products (status, id DESC);

CREATE INDEX IF NOT EXISTS idx_products_status_category
    ON products (status, category);

CREATE INDEX IF NOT EXISTS idx_products_status_brand
    ON products (status, brand);

CREATE INDEX IF NOT EXISTS idx_products_seller_status
    ON products (seller_profile_id, status);

CREATE INDEX IF NOT EXISTS idx_product_views_product_viewed
    ON product_views (product_id, viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_views_user_viewed
    ON product_views (user_id, viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activities_user_product_type
    ON user_activities (user_id, product_id, activity_type);

CREATE INDEX IF NOT EXISTS idx_user_activities_product_type
    ON user_activities (product_id, activity_type);

CREATE INDEX IF NOT EXISTS idx_user_activities_timestamp
    ON user_activities (timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_cart_items_user
    ON cart_items (user_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_product
    ON cart_items (product_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_user_product_variant
    ON cart_items (user_id, product_id, variant_id);

CREATE INDEX IF NOT EXISTS idx_wishlists_user
    ON wishlists (user_id);

CREATE INDEX IF NOT EXISTS idx_wishlists_product
    ON wishlists (product_id);

CREATE INDEX IF NOT EXISTS idx_reviews_product_created
    ON reviews (product_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reviews_user_created
    ON reviews (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payments_state_updated
    ON payments (state, updated_at DESC);
