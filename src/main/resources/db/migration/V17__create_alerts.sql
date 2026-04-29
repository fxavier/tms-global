CREATE TABLE alerts (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_type  VARCHAR(50)  NOT NULL
        CHECK (alert_type IN ('DOCUMENT_EXPIRING', 'DOCUMENT_EXPIRED', 'MAINTENANCE_DUE', 'MAINTENANCE_OVERDUE', 'CHECKLIST_FAILURE')),
    severity    VARCHAR(20)  NOT NULL
        CHECK (severity IN ('INFO', 'AVISO', 'CRITICO')),
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   UUID         NOT NULL,
    title       VARCHAR(300) NOT NULL,
    message     TEXT         NOT NULL,
    is_resolved BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100) NOT NULL,
    updated_by  VARCHAR(100) NOT NULL
);

CREATE INDEX idx_alerts_entity
    ON alerts(entity_type, entity_id);

CREATE INDEX idx_alerts_is_resolved
    ON alerts(is_resolved)
    WHERE is_resolved = FALSE;

CREATE INDEX idx_alerts_severity
    ON alerts(severity)
    WHERE is_resolved = FALSE;

CREATE UNIQUE INDEX idx_alerts_dedup
    ON alerts(alert_type, entity_id)
    WHERE is_resolved = FALSE;

CREATE TABLE alert_configurations (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_type           VARCHAR(50)  NOT NULL,
    entity_type          VARCHAR(50)  NOT NULL,
    days_before_warning  INTEGER      NOT NULL DEFAULT 30,
    days_before_critical INTEGER      NOT NULL DEFAULT 7,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100) NOT NULL,
    CONSTRAINT uk_alert_config_type_entity UNIQUE (alert_type, entity_type)
);

INSERT INTO alert_configurations (
    alert_type,
    entity_type,
    days_before_warning,
    days_before_critical,
    is_active,
    created_by,
    updated_by
) VALUES
    ('DOCUMENT_EXPIRING', 'VEHICLE_DOCUMENT', 30, 7, TRUE, 'system', 'system'),
    ('DOCUMENT_EXPIRING', 'DRIVER_DOCUMENT', 30, 7, TRUE, 'system', 'system'),
    ('MAINTENANCE_DUE', 'MAINTENANCE_RECORD', 30, 7, TRUE, 'system', 'system');
