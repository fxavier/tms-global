CREATE TABLE files (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    original_filename VARCHAR(255) NOT NULL,
    storage_key       VARCHAR(500) NOT NULL UNIQUE,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL CHECK (size_bytes >= 0),
    uploaded_by       VARCHAR(100) NOT NULL,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
