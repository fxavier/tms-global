CREATE INDEX idx_drivers_status
    ON drivers(status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_drivers_license_expiry
    ON drivers(license_expiry_date)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_drivers_location
    ON drivers(activity_location)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_driver_docs_driver
    ON driver_documents(driver_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_driver_docs_expiry
    ON driver_documents(expiry_date)
    WHERE deleted_at IS NULL AND status != 'CANCELADO';
