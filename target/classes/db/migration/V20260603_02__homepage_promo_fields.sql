-- Homepage promo fields: add description and banner_image to promo_codes
DO $$
BEGIN
    IF to_regclass('promo_codes') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'promo_codes' AND column_name = 'description'
        ) THEN
            ALTER TABLE promo_codes ADD COLUMN description TEXT;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'promo_codes' AND column_name = 'banner_image'
        ) THEN
            ALTER TABLE promo_codes ADD COLUMN banner_image VARCHAR(1000);
        END IF;
    END IF;
END $$;
