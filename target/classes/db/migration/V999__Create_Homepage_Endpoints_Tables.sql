-- Migration for Homepage Endpoints (Best Sellers, Top Rated, Most Wishlisted, Trending, Recommendations, Analytics, Admin Statistics, Announcements)
-- All CREATE TABLE statements use IF NOT EXISTS.
-- All CREATE INDEX statements use IF NOT EXISTS (idempotent).
-- Indexes on Hibernate-managed tables are wrapped in DO blocks that verify the table exists first.

-- Create popular_searches table for analytics
CREATE TABLE IF NOT EXISTS popular_searches (
    id BIGSERIAL PRIMARY KEY,
    search_keyword VARCHAR(255) NOT NULL UNIQUE,
    search_count BIGINT NOT NULL DEFAULT 0,
    unique_users BIGINT NOT NULL DEFAULT 0,
    conversion_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    last_searched_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for popular_searches
CREATE INDEX IF NOT EXISTS idx_search_count  ON popular_searches(search_count DESC);
CREATE INDEX IF NOT EXISTS idx_last_searched ON popular_searches(last_searched_at DESC);
CREATE INDEX IF NOT EXISTS idx_keyword       ON popular_searches(search_keyword);

-- Create announcements table
CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    content TEXT,
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    image_url VARCHAR(500),
    action_url VARCHAR(500),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for announcements
CREATE INDEX IF NOT EXISTS idx_status        ON announcements(status);
CREATE INDEX IF NOT EXISTS idx_is_active     ON announcements(is_active);
CREATE INDEX IF NOT EXISTS idx_display_order ON announcements(display_order DESC);
CREATE INDEX IF NOT EXISTS idx_end_date      ON announcements(end_date);

-- Indexes on Hibernate-managed tables — wrapped in DO blocks to ensure
-- the tables exist before indexing (in case ddl-auto didn't run for some reason).

DO $$
BEGIN
    -- Indexes for best-sellers query (order_items)
    IF to_regclass('order_items') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
    END IF;

    -- Indexes for top-rated query (reviews)
    IF to_regclass('reviews') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
        CREATE INDEX IF NOT EXISTS idx_reviews_rating     ON reviews(rating DESC);
    END IF;

    -- Indexes for most-wishlisted query (wishlists)
    IF to_regclass('wishlists') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_wishlists_product_id ON wishlists(product_id);
    END IF;

    -- Indexes for trending query (product_views)
    IF to_regclass('product_views') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_product_views_product_id ON product_views(product_id);
        CREATE INDEX IF NOT EXISTS idx_product_views_viewed_at  ON product_views(viewed_at DESC);
    END IF;

    -- Indexes for product queries
    IF to_regclass('products') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_products_status           ON products(status);
        CREATE INDEX IF NOT EXISTS idx_products_category         ON products(category);
        CREATE INDEX IF NOT EXISTS idx_products_seller_profile_id ON products(seller_profile_id);
    END IF;

    -- Indexes for seller_profiles
    IF to_regclass('seller_profiles') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_seller_profiles_user_id ON seller_profiles(user_id);
    END IF;

    -- Indexes for users
    IF to_regclass('users') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_users_role       ON users(role);
        CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);
    END IF;

    -- Indexes for orders
    IF to_regclass('orders') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
        CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
    END IF;

    -- Indexes for product_images
    IF to_regclass('product_images') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
    END IF;

    -- Indexes for product_variants
    IF to_regclass('product_variants') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants(product_id);
        CREATE INDEX IF NOT EXISTS idx_product_variants_active     ON product_variants(active);
    END IF;
END $$;
