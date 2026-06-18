-- Refund inspection, payment, and risk fields
DO $$
BEGIN
    IF to_regclass('refunds') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='inspection_condition') THEN
            ALTER TABLE refunds ADD COLUMN inspection_condition VARCHAR(40);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='inspection_notes') THEN
            ALTER TABLE refunds ADD COLUMN inspection_notes TEXT;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='inspected_at') THEN
            ALTER TABLE refunds ADD COLUMN inspected_at TIMESTAMP;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='risk_score') THEN
            ALTER TABLE refunds ADD COLUMN risk_score INTEGER;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='risk_level') THEN
            ALTER TABLE refunds ADD COLUMN risk_level VARCHAR(20);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='payment_stage') THEN
            ALTER TABLE refunds ADD COLUMN payment_stage VARCHAR(40);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='provider_reference') THEN
            ALTER TABLE refunds ADD COLUMN provider_reference VARCHAR(255);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='refunds' AND column_name='gateway_event_at') THEN
            ALTER TABLE refunds ADD COLUMN gateway_event_at TIMESTAMP;
        END IF;

        CREATE INDEX IF NOT EXISTS idx_refunds_risk_level    ON refunds(risk_level);
        CREATE INDEX IF NOT EXISTS idx_refunds_payment_stage ON refunds(payment_stage);
    END IF;
END $$;
