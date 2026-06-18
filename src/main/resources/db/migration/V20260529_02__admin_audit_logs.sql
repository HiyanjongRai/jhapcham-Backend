CREATE TABLE IF NOT EXISTS admin_audit_logs (
    audit_id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT,
    actor_username VARCHAR(100),
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id BIGINT,
    summary VARCHAR(500) NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF to_regclass('users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint
           WHERE conname = 'fk_admin_audit_logs_actor'
             AND conrelid = 'admin_audit_logs'::regclass
       ) THEN
        ALTER TABLE admin_audit_logs ADD CONSTRAINT fk_admin_audit_logs_actor
            FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_admin_audit_actor ON admin_audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_target ON admin_audit_logs(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_action_time ON admin_audit_logs(action, created_at);
