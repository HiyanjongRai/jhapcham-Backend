-- Migration: Add missing Campaign fields
-- Created: 2025-06-01
-- Purpose: Ensure campaigns table exists and add description, discountValue, and maxProducts columns
-- Note: campaigns table may be created by Hibernate ddl-auto before this script if the DB is fresh.
--       We use CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS so this is safe either way.

-- Create the campaigns table if Hibernate hasn't already created it
CREATE TABLE IF NOT EXISTS campaigns (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    type        VARCHAR(50)  NOT NULL,
    start_time  TIMESTAMP    NOT NULL,
    end_time    TIMESTAMP    NOT NULL,
    discount_type  VARCHAR(50)    NOT NULL,
    discount_value DECIMAL(10,2),
    max_products   INT            DEFAULT 1000,
    status      VARCHAR(50)  NOT NULL,
    priority    INT          NOT NULL,
    image_path  VARCHAR(500)
);

-- Idempotently add the three columns (no-op if they already exist)
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS description    TEXT;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS discount_value DECIMAL(10,2);
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS max_products   INT DEFAULT 1000;

-- Backfill default values for any pre-existing rows
UPDATE campaigns
SET discount_value = 0
WHERE discount_value IS NULL AND discount_type IS NOT NULL;

UPDATE campaigns
SET max_products = 1000
WHERE max_products IS NULL;

