CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type     VARCHAR(100)  NOT NULL,
    entity_id       UUID,
    operation       VARCHAR(30)   NOT NULL,
    performed_by    VARCHAR(100)  NOT NULL,
    ip_address      VARCHAR(64)   NOT NULL,
    previous_values JSONB,
    new_values      JSONB,
    occurred_at     TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_audit_logs_entity_type
    ON audit_logs(entity_type);

CREATE INDEX idx_audit_logs_operation
    ON audit_logs(operation);

CREATE INDEX idx_audit_logs_performed_by
    ON audit_logs(performed_by);

CREATE INDEX idx_audit_logs_occurred_at
    ON audit_logs(occurred_at);
