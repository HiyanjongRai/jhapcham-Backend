-- Variant discount/sale fields
DO $$
BEGIN
    IF to_regclass('product_variants') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='product_variants' AND column_name='on_sale') THEN
            ALTER TABLE product_variants ADD COLUMN on_sale BOOLEAN NOT NULL DEFAULT FALSE;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='product_variants' AND column_name='discount_price') THEN
            ALTER TABLE product_variants ADD COLUMN discount_price NUMERIC(38, 2);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='product_variants' AND column_name='sale_percentage') THEN
            ALTER TABLE product_variants ADD COLUMN sale_percentage NUMERIC(38, 2);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='product_variants' AND column_name='sale_price') THEN
            ALTER TABLE product_variants ADD COLUMN sale_price NUMERIC(38, 2);
        END IF;
    END IF;

    IF to_regclass('products') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='products' AND column_name='sale_start_time') THEN
            ALTER TABLE products ADD COLUMN sale_start_time TIMESTAMP;
        END IF;
    END IF;
END $$;
