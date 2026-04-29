CREATE TABLE activities (
    id                        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code                      VARCHAR(30)  NOT NULL UNIQUE,
    title                     VARCHAR(300) NOT NULL,
    activity_type             VARCHAR(100) NOT NULL,
    location                  VARCHAR(300) NOT NULL,
    planned_start             TIMESTAMPTZ  NOT NULL,
    planned_end               TIMESTAMPTZ  NOT NULL,
    actual_start              TIMESTAMPTZ,
    actual_end                TIMESTAMPTZ,
    priority                  VARCHAR(20)  NOT NULL DEFAULT 'NORMAL'
        CHECK (priority IN ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'PLANEADA'
        CHECK (status IN ('PLANEADA', 'EM_CURSO', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA')),
    vehicle_id                UUID,
    driver_id                 UUID,
    description               TEXT,
    notes                     TEXT,
    rh_override_justification TEXT,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(100) NOT NULL,
    updated_by                VARCHAR(100) NOT NULL,
    deleted_at                TIMESTAMPTZ,
    deleted_by                VARCHAR(100),
    CONSTRAINT fk_activities_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,
    CONSTRAINT fk_activities_driver FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL
);

CREATE INDEX idx_activities_status
    ON activities(status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_activities_vehicle
    ON activities(vehicle_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_activities_driver
    ON activities(driver_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_activities_planned_start
    ON activities(planned_start)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_activities_code
    ON activities(code);
