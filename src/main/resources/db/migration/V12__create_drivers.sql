CREATE TABLE drivers (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name           VARCHAR(200) NOT NULL,
    phone               VARCHAR(30),
    address             TEXT,
    id_number           VARCHAR(50)  NOT NULL UNIQUE,
    license_number      VARCHAR(50)  NOT NULL UNIQUE,
    license_category    VARCHAR(30),
    license_issue_date  DATE,
    license_expiry_date DATE,
    activity_location   VARCHAR(200),
    status              VARCHAR(30)  NOT NULL DEFAULT 'ATIVO'
        CHECK (status IN ('ATIVO', 'INATIVO', 'SUSPENSO')),
    notes               TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100)
);
