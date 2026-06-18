-- Add return_required column to refunds table
ALTER TABLE refunds ADD COLUMN return_required BOOLEAN DEFAULT TRUE;
