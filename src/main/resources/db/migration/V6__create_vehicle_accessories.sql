CREATE TABLE vehicle_accessories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id      UUID        NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    accessory_type  VARCHAR(50) NOT NULL
        CHECK (accessory_type IN ('MACACO', 'RODA_SOBRESSALENTE', 'TRIANGULO', 'EXTINTOR', 'KIT_PRIMEIROS_SOCORROS', 'COLETE_REFLETOR', 'OUTRO')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PRESENTE'
        CHECK (status IN ('PRESENTE', 'AUSENTE', 'DANIFICADO')),
    last_checked_at TIMESTAMPTZ,
    last_checked_by VARCHAR(100),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    CONSTRAINT uk_vehicle_accessories_vehicle_type UNIQUE (vehicle_id, accessory_type)
);
