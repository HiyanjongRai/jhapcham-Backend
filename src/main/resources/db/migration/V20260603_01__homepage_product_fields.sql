-- Homepage product fields: add featured flag and created_at tracking
DO $$
BEGIN
    IF to_regclass('products') IS NOT NULL THEN
        -- Add featured column if not already present
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'featured'
        ) THEN
            ALTER TABLE products ADD COLUMN featured BOOLEAN NOT NULL DEFAULT false;
        END IF;

        -- Add created_at column if not already present
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'created_at'
        ) THEN
            ALTER TABLE products ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
        END IF;

        CREATE INDEX IF NOT EXISTS idx_products_status_created_at ON products(status, created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_products_status_featured    ON products(status, featured);
    END IF;
END $$;
