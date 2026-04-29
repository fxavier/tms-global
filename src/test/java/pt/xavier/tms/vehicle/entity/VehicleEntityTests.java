package pt.xavier.tms.vehicle.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pt.xavier.tms.shared.enums.ChecklistItemStatus;

class VehicleEntityTests {

    @Test
    void vehicleSoftDeleteStoresAuditFields() {
        Vehicle vehicle = new Vehicle();

        vehicle.softDelete("admin@tms.local");

        assertThat(vehicle.getDeletedAt()).isNotNull();
        assertThat(vehicle.getDeletedBy()).isEqualTo("admin@tms.local");
    }

    @Test
    void vehicleDocumentSoftDeleteStoresAuditFields() {
        VehicleDocument document = new VehicleDocument();

        document.softDelete("admin@tms.local");

        assertThat(document.getDeletedAt()).isNotNull();
        assertThat(document.getDeletedBy()).isEqualTo("admin@tms.local");
    }

    @Test
    void checklistInspectionDetectsCriticalFailures() {
        ChecklistInspection inspection = new ChecklistInspection();
        ChecklistInspectionItem okCriticalItem = item(true, ChecklistItemStatus.OK);
        ChecklistInspectionItem failedCriticalItem = item(true, ChecklistItemStatus.AVARIA);
        ChecklistInspectionItem failedNonCriticalItem = item(false, ChecklistItemStatus.FALTA);

        inspection.getItems().add(okCriticalItem);
        inspection.getItems().add(failedCriticalItem);
        inspection.getItems().add(failedNonCriticalItem);

        assertThat(inspection.hasCriticalFailures()).isTrue();
    }

    @Test
    void checklistInspectionIgnoresNonCriticalFailures() {
        ChecklistInspection inspection = new ChecklistInspection();
        inspection.getItems().add(item(false, ChecklistItemStatus.AVARIA));
        inspection.getItems().add(item(true, ChecklistItemStatus.OK));

        assertThat(inspection.hasCriticalFailures()).isFalse();
    }

    private static ChecklistInspectionItem item(boolean critical, ChecklistItemStatus status) {
        ChecklistInspectionItem item = new ChecklistInspectionItem();
        item.setCritical(critical);
        item.setStatus(status);
        return item;
    }
}
