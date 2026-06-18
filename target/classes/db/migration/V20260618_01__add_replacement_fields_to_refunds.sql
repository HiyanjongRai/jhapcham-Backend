-- Add replacement fields to refunds table
ALTER TABLE refunds ADD COLUMN replacement_courier VARCHAR(150);
ALTER TABLE refunds ADD COLUMN replacement_tracking_number VARCHAR(150);
ALTER TABLE refunds ADD COLUMN replacement_shipped_at TIMESTAMP;
