CREATE TABLE driver_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id       UUID         NOT NULL,
    document_type   VARCHAR(40)  NOT NULL
        CHECK (document_type IN ('CARTA_CONDUCAO', 'BILHETE_IDENTIDADE', 'OUTRO')),
    document_number VARCHAR(100),
    issue_date      DATE,
    expiry_date     DATE,
    issuing_entity  VARCHAR(200),
    category        VARCHAR(30),
    status          VARCHAR(40)  NOT NULL DEFAULT 'VALIDO'
        CHECK (status IN ('VALIDO', 'EXPIRADO', 'PENDENTE_RENOVACAO', 'CANCELADO')),
    notes           TEXT,
    file_id         UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(100),
    CONSTRAINT fk_driver_documents_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    CONSTRAINT fk_driver_documents_file FOREIGN KEY (file_id) REFERENCES files(id)
);
