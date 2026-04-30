ALTER TABLE drivers
    ADD COLUMN employee_id UUID;

ALTER TABLE drivers
    ADD CONSTRAINT fk_drivers_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id);

CREATE INDEX idx_drivers_employee ON drivers (employee_id);
