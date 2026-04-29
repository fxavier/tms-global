CREATE INDEX idx_vehicles_plate
    ON vehicles(plate);

CREATE INDEX idx_vehicles_status
    ON vehicles(status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_vehicles_plate_trgm
    ON vehicles USING gin(plate gin_trgm_ops);

CREATE INDEX idx_vehicle_docs_expiry
    ON vehicle_documents(expiry_date)
    WHERE deleted_at IS NULL AND status != 'CANCELADO';

CREATE INDEX idx_vehicle_docs_status
    ON vehicle_documents(status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_vehicle_accessories_vehicle
    ON vehicle_accessories(vehicle_id);

CREATE INDEX idx_maintenance_vehicle
    ON maintenance_records(vehicle_id);

CREATE INDEX idx_maintenance_next_date
    ON maintenance_records(next_maintenance_date)
    WHERE next_maintenance_date IS NOT NULL;

CREATE INDEX idx_checklist_inspections_vehicle
    ON checklist_inspections(vehicle_id);
