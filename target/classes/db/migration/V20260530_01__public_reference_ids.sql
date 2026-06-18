-- Public, searchable reference IDs for reports, refunds, and disputes.
-- Format examples:
-- PRD-RPT-20260530-H72KQ91A, SLR-RPT-20260530-K9M3P2WX, REF-20260530-B4N8X2KP

CREATE OR REPLACE FUNCTION jhapcham_public_ref(prefix text, created timestamp)
RETURNS text AS $$
DECLARE
    alphabet text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    suffix text := '';
    i int;
BEGIN
    FOR i IN 1..12 LOOP
        suffix := suffix || substr(alphabet, floor(random() * length(alphabet) + 1)::int, 1);
    END LOOP;

    RETURN prefix || '-' || to_char(COALESCE(created, now()), 'YYYYMMDD') || '-' || suffix;
END;
$$ LANGUAGE plpgsql;

-- Add columns (all IF NOT EXISTS - safe if Hibernate already created them)
ALTER TABLE product_reports ADD COLUMN IF NOT EXISTS public_reference_id VARCHAR(50);
ALTER TABLE seller_reports ADD COLUMN IF NOT EXISTS public_reference_id VARCHAR(50);
ALTER TABLE customer_reports ADD COLUMN IF NOT EXISTS public_reference_id VARCHAR(50);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS public_reference_id VARCHAR(50);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS public_reference_id VARCHAR(50);

-- Backfill NULL reference IDs only (safe for both fresh and existing databases)
UPDATE product_reports SET public_reference_id = jhapcham_public_ref('PRD-RPT', created_at) WHERE public_reference_id IS NULL;
UPDATE seller_reports  SET public_reference_id = jhapcham_public_ref('SLR-RPT', created_at) WHERE public_reference_id IS NULL;
UPDATE customer_reports SET public_reference_id = jhapcham_public_ref('CUS-RPT', created_at) WHERE public_reference_id IS NULL;
UPDATE refunds         SET public_reference_id = jhapcham_public_ref('REF',     created_at)  WHERE public_reference_id IS NULL;
UPDATE disputes        SET public_reference_id = jhapcham_public_ref('DSP',     created_at)  WHERE public_reference_id IS NULL;

-- Set NOT NULL only where the column is still nullable (idempotent via DO block)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'product_reports'
          AND column_name = 'public_reference_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE product_reports ALTER COLUMN public_reference_id SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'seller_reports'
          AND column_name = 'public_reference_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE seller_reports ALTER COLUMN public_reference_id SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'customer_reports'
          AND column_name = 'public_reference_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE customer_reports ALTER COLUMN public_reference_id SET NOT NULL;
    END IF;

    -- refunds.public_reference_id is managed by Hibernate and may already be NOT NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'refunds'
          AND column_name = 'public_reference_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE refunds ALTER COLUMN public_reference_id SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'disputes'
          AND column_name = 'public_reference_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE disputes ALTER COLUMN public_reference_id SET NOT NULL;
    END IF;
END $$;

-- Add UNIQUE constraints only if they don't already exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_product_reports_public_reference_id') THEN
        ALTER TABLE product_reports ADD CONSTRAINT uk_product_reports_public_reference_id UNIQUE (public_reference_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_seller_reports_public_reference_id') THEN
        ALTER TABLE seller_reports ADD CONSTRAINT uk_seller_reports_public_reference_id UNIQUE (public_reference_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_customer_reports_public_reference_id') THEN
        ALTER TABLE customer_reports ADD CONSTRAINT uk_customer_reports_public_reference_id UNIQUE (public_reference_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_refunds_public_reference_id') THEN
        ALTER TABLE refunds ADD CONSTRAINT uk_refunds_public_reference_id UNIQUE (public_reference_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_disputes_public_reference_id') THEN
        ALTER TABLE disputes ADD CONSTRAINT uk_disputes_public_reference_id UNIQUE (public_reference_id);
    END IF;
END $$;

-- Indexes (IF NOT EXISTS - safe)
CREATE INDEX IF NOT EXISTS idx_product_reports_public_reference_id  ON product_reports(public_reference_id);
CREATE INDEX IF NOT EXISTS idx_seller_reports_public_reference_id   ON seller_reports(public_reference_id);
CREATE INDEX IF NOT EXISTS idx_customer_reports_public_reference_id ON customer_reports(public_reference_id);
CREATE INDEX IF NOT EXISTS idx_refunds_public_reference_id          ON refunds(public_reference_id);
CREATE INDEX IF NOT EXISTS idx_disputes_public_reference_id         ON disputes(public_reference_id);

DROP FUNCTION IF EXISTS jhapcham_public_ref(text, timestamp);
