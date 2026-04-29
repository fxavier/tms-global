CREATE TABLE activity_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id     UUID         NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    previous_status VARCHAR(20),
    new_status      VARCHAR(20),
    performed_by    VARCHAR(100) NOT NULL,
    performed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    CONSTRAINT fk_activity_events_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT chk_activity_events_previous_status CHECK (
        previous_status IS NULL OR previous_status IN ('PLANEADA', 'EM_CURSO', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA')
    ),
    CONSTRAINT chk_activity_events_new_status CHECK (
        new_status IS NULL OR new_status IN ('PLANEADA', 'EM_CURSO', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA')
    )
);

CREATE INDEX idx_activity_events_activity
    ON activity_events(activity_id);
