CREATE TABLE vehicles (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plate               VARCHAR(20)  NOT NULL UNIQUE,
    brand               VARCHAR(100) NOT NULL,
    model               VARCHAR(100) NOT NULL,
    vehicle_type        VARCHAR(100) NOT NULL,
    capacity            INTEGER      NOT NULL CHECK (capacity > 0),
    activity_location   VARCHAR(200) NOT NULL,
    activity_start_date DATE         NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'DISPONIVEL'
        CHECK (status IN ('DISPONIVEL', 'INDISPONIVEL', 'EM_MANUTENCAO', 'ABATIDA')),
    current_driver_id   UUID,
    notes               TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100)
);
