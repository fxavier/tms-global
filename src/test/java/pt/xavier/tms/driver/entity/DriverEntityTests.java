package pt.xavier.tms.driver.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DriverEntityTests {

    @Test
    void driverSoftDeleteStoresAuditFields() {
        Driver driver = new Driver();

        driver.softDelete("admin@tms.local");

        assertThat(driver.getDeletedAt()).isNotNull();
        assertThat(driver.getDeletedBy()).isEqualTo("admin@tms.local");
    }

    @Test
    void driverDocumentSoftDeleteStoresAuditFields() {
        DriverDocument document = new DriverDocument();

        document.softDelete("admin@tms.local");

        assertThat(document.getDeletedAt()).isNotNull();
        assertThat(document.getDeletedBy()).isEqualTo("admin@tms.local");
    }
}
