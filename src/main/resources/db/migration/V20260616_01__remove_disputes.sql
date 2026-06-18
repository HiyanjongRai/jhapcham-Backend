-- V20260616_01__remove_disputes.sql
-- Migration to clean up and remove the disputes system database tables and columns

DROP TABLE IF EXISTS dispute_decisions CASCADE;
DROP TABLE IF EXISTS dispute_messages CASCADE;
DROP TABLE IF EXISTS disputes CASCADE;

-- Drop dispute metrics from seller_refund_metrics table
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'seller_refund_metrics') THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'seller_refund_metrics' AND column_name = 'disputed_refunds'
        ) THEN
            ALTER TABLE seller_refund_metrics DROP COLUMN disputed_refunds;
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'seller_refund_metrics' AND column_name = 'dispute_rate'
        ) THEN
            ALTER TABLE seller_refund_metrics DROP COLUMN dispute_rate;
        END IF;
    END IF;
END $$;
