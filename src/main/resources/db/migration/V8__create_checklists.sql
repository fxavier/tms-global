CREATE TABLE checklist_templates (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_type VARCHAR(100) NOT NULL,
    name         VARCHAR(200) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL
);

CREATE TABLE checklist_template_items (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id   UUID         NOT NULL REFERENCES checklist_templates(id) ON DELETE CASCADE,
    item_name     VARCHAR(200) NOT NULL,
    is_critical   BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL
);

CREATE TABLE checklist_inspections (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id   UUID         NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    activity_id  UUID,
    template_id  UUID         NOT NULL REFERENCES checklist_templates(id),
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMPTZ  NOT NULL,
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL
);

CREATE TABLE checklist_inspection_items (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inspection_id    UUID         NOT NULL REFERENCES checklist_inspections(id) ON DELETE CASCADE,
    template_item_id UUID REFERENCES checklist_template_items(id) ON DELETE SET NULL,
    item_name        VARCHAR(200) NOT NULL,
    is_critical      BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('OK', 'AVARIA', 'FALTA')),
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL
);
