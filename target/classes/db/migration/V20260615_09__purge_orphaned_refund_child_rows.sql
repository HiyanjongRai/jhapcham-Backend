-- ===================================================================
-- Migration: V20260615_09__purge_orphaned_refund_child_rows.sql
-- Purpose  : Clean up orphaned rows in refund child tables.
--
-- Root cause: On migration from old schema, refund_logs/refund_items/
-- refund_evidence/refund_timeline_events may contain rows whose
-- refund_id no longer exists in the refunds table (test data, dropped
-- rows, or schema resets). Hibernate's ddl-auto=update cannot add the
-- FK constraint while orphaned rows exist, producing a WARN on startup:
--   "insert or update on table refund_logs violates foreign key constraint"
--
-- This migration removes all orphaned child rows so that Hibernate can
-- successfully enforce referential integrity on the next application start.
-- ===================================================================

-- ── 1. refund_logs ────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF to_regclass('refund_logs') IS NOT NULL AND to_regclass('refunds') IS NOT NULL THEN
        DELETE FROM refund_logs
        WHERE refund_id IS NOT NULL
          AND refund_id NOT IN (SELECT refund_id FROM refunds);

        RAISE NOTICE '[V09] Purged orphaned refund_logs rows.';
    END IF;
END $$;

-- ── 2. refund_items ───────────────────────────────────────────────────────────
DO $$
BEGIN
    IF to_regclass('refund_items') IS NOT NULL AND to_regclass('refunds') IS NOT NULL THEN
        DELETE FROM refund_items
        WHERE refund_id IS NOT NULL
          AND refund_id NOT IN (SELECT refund_id FROM refunds);

        RAISE NOTICE '[V09] Purged orphaned refund_items rows.';
    END IF;
END $$;

-- ── 3. refund_evidence ────────────────────────────────────────────────────────
DO $$
BEGIN
    IF to_regclass('refund_evidence') IS NOT NULL AND to_regclass('refunds') IS NOT NULL THEN
        DELETE FROM refund_evidence
        WHERE refund_id IS NOT NULL
          AND refund_id NOT IN (SELECT refund_id FROM refunds);

        RAISE NOTICE '[V09] Purged orphaned refund_evidence rows.';
    END IF;
END $$;

-- ── 4. refund_timeline_events ─────────────────────────────────────────────────
DO $$
BEGIN
    IF to_regclass('refund_timeline_events') IS NOT NULL AND to_regclass('refunds') IS NOT NULL THEN
        DELETE FROM refund_timeline_events
        WHERE refund_id IS NOT NULL
          AND refund_id NOT IN (SELECT refund_id FROM refunds);

        RAISE NOTICE '[V09] Purged orphaned refund_timeline_events rows.';
    END IF;
END $$;

-- ── 5. seller_liability_ledger ────────────────────────────────────────────────
DO $$
BEGIN
    IF to_regclass('seller_liability_ledger') IS NOT NULL AND to_regclass('refunds') IS NOT NULL THEN
        DELETE FROM seller_liability_ledger
        WHERE refund_id IS NOT NULL
          AND refund_id NOT IN (SELECT refund_id FROM refunds);

        RAISE NOTICE '[V09] Purged orphaned seller_liability_ledger rows.';
    END IF;
END $$;

-- ── 6. wallet_transactions (refund_id is nullable — only nullify orphaned refs) ─
DO $$
BEGIN
    IF to_regclass('wallet_transactions') IS NOT NULL AND to_regclass('refunds') IS NOT NULL THEN
        UPDATE wallet_transactions
           SET refund_id = NULL
         WHERE refund_id IS NOT NULL
           AND refund_id NOT IN (SELECT refund_id FROM refunds);

        RAISE NOTICE '[V09] Nullified orphaned wallet_transactions.refund_id references.';
    END IF;
END $$;

-- ── 7. Now drop the duplicate Flyway-created FK on refund_logs if it exists ───
-- Hibernate will recreate it cleanly after the orphaned rows are gone.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'refund_logs'
          AND constraint_name = 'fk_refund_logs_refund'
    ) THEN
        ALTER TABLE refund_logs DROP CONSTRAINT fk_refund_logs_refund;
        RAISE NOTICE '[V09] Dropped fk_refund_logs_refund — Hibernate will recreate it.';
    END IF;
END $$;
