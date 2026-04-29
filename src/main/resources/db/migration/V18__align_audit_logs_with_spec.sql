ALTER TABLE audit_logs
    RENAME COLUMN occurred_at TO created_at;

ALTER TABLE audit_logs
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS chk_audit_logs_operation;

ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_logs_operation
        CHECK (operation IN ('CRIACAO', 'ATUALIZACAO', 'ELIMINACAO'));

DROP INDEX IF EXISTS idx_audit_logs_entity_type;
DROP INDEX IF EXISTS idx_audit_logs_operation;
DROP INDEX IF EXISTS idx_audit_logs_performed_by;
DROP INDEX IF EXISTS idx_audit_logs_occurred_at;

CREATE INDEX IF NOT EXISTS idx_audit_entity
    ON audit_logs(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_performed_by
    ON audit_logs(performed_by);

CREATE INDEX IF NOT EXISTS idx_audit_performed_at
    ON audit_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_operation
    ON audit_logs(operation);
