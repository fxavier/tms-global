package pt.xavier.tms.vehicle.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pt.xavier.tms.shared.enums.AccessoryStatus;
import pt.xavier.tms.shared.enums.AccessoryType;
import pt.xavier.tms.shared.enums.ChecklistItemStatus;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.MaintenanceType;
import pt.xavier.tms.shared.enums.VehicleDocumentType;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
class VehicleJpaMappingIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_vehicle_test")
            .withUsername("tms_vehicle_test")
            .withPassword("tms_vehicle_test");

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    @Transactional
    void persistsVehicleEntitiesAgainstFlywaySchema() {
        FileRecord file = fileRecord();
        entityManager.persist(file);

        Vehicle vehicle = vehicle();

        VehicleDocument document = vehicleDocument(vehicle, file);
        VehicleAccessory accessory = vehicleAccessory(vehicle);
        MaintenanceRecord maintenanceRecord = maintenanceRecord(vehicle);

        vehicle.getDocuments().add(document);
        vehicle.getAccessories().add(accessory);
        vehicle.getMaintenanceRecords().add(maintenanceRecord);

        entityManager.persist(vehicle);

        ChecklistTemplate template = checklistTemplate();
        ChecklistTemplateItem templateItem = checklistTemplateItem(template);
        template.getItems().add(templateItem);
        entityManager.persist(template);

        ChecklistInspection inspection = checklistInspection(vehicle, template);
        ChecklistInspectionItem inspectionItem = checklistInspectionItem(inspection, templateItem);
        inspection.getItems().add(inspectionItem);
        entityManager.persist(inspection);

        entityManager.flush();
        entityManager.clear();

        Vehicle persisted = entityManager.find(Vehicle.class, vehicle.getId());
        ChecklistInspection persistedInspection = entityManager.find(ChecklistInspection.class, inspection.getId());

        assertThat(persisted).isNotNull();
        assertThat(persisted.getCreatedBy()).isEqualTo("system");
        assertThat(persistedInspection.hasCriticalFailures()).isTrue();
    }

    private static FileRecord fileRecord() {
        FileRecord file = new FileRecord();
        file.setOriginalFilename("insurance.pdf");
        file.setStorageKey("vehicle-documents/insurance.pdf");
        file.setContentType("application/pdf");
        file.setSizeBytes(1024L);
        file.setUploadedBy("system");
        file.setUploadedAt(Instant.now());
        return file;
    }

    private static Vehicle vehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate("AA-00-AA");
        vehicle.setBrand("Mercedes");
        vehicle.setModel("Actros");
        vehicle.setVehicleType("TRUCK");
        vehicle.setCapacity(12000);
        vehicle.setActivityLocation("Lisboa");
        vehicle.setActivityStartDate(LocalDate.now());
        vehicle.setNotes("Operational vehicle");
        return vehicle;
    }

    private static VehicleDocument vehicleDocument(Vehicle vehicle, FileRecord file) {
        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicle);
        document.setDocumentType(VehicleDocumentType.SEGURO);
        document.setDocumentNumber("SEG-001");
        document.setIssueDate(LocalDate.now().minusMonths(1));
        document.setExpiryDate(LocalDate.now().plusYears(1));
        document.setIssuingEntity("Insurer");
        document.setStatus(DocumentStatus.VALIDO);
        document.setFile(file);
        return document;
    }

    private static VehicleAccessory vehicleAccessory(Vehicle vehicle) {
        VehicleAccessory accessory = new VehicleAccessory();
        accessory.setVehicle(vehicle);
        accessory.setAccessoryType(AccessoryType.EXTINTOR);
        accessory.setStatus(AccessoryStatus.PRESENTE);
        accessory.setLastCheckedAt(Instant.now());
        accessory.setLastCheckedBy("system");
        return accessory;
    }

    private static MaintenanceRecord maintenanceRecord(Vehicle vehicle) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setMaintenanceType(MaintenanceType.PREVENTIVA);
        record.setPerformedAt(LocalDate.now());
        record.setMileageAtService(10000);
        record.setDescription("Preventive service");
        record.setSupplier("Workshop");
        record.setTotalCost(BigDecimal.valueOf(250.00));
        record.setNextMaintenanceDate(LocalDate.now().plusMonths(6));
        record.setNextMaintenanceMileage(20000);
        record.setResponsibleUser("system");
        return record;
    }

    private static ChecklistTemplate checklistTemplate() {
        ChecklistTemplate template = new ChecklistTemplate();
        template.setVehicleType("TRUCK");
        template.setName("Truck inspection");
        template.setActive(true);
        return template;
    }

    private static ChecklistTemplateItem checklistTemplateItem(ChecklistTemplate template) {
        ChecklistTemplateItem item = new ChecklistTemplateItem();
        item.setTemplate(template);
        item.setItemName("Brakes");
        item.setCritical(true);
        item.setDisplayOrder(1);
        return item;
    }

    private static ChecklistInspection checklistInspection(Vehicle vehicle, ChecklistTemplate template) {
        ChecklistInspection inspection = new ChecklistInspection();
        inspection.setVehicle(vehicle);
        inspection.setTemplate(template);
        inspection.setPerformedBy("driver-1");
        inspection.setPerformedAt(Instant.now());
        return inspection;
    }

    private static ChecklistInspectionItem checklistInspectionItem(
            ChecklistInspection inspection,
            ChecklistTemplateItem templateItem
    ) {
        ChecklistInspectionItem item = new ChecklistInspectionItem();
        item.setInspection(inspection);
        item.setTemplateItem(templateItem);
        item.setItemName("Brakes");
        item.setCritical(true);
        item.setStatus(ChecklistItemStatus.AVARIA);
        return item;
    }
}
