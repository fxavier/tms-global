package pt.xavier.tms.vehicle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.entity.ChecklistInspectionItem;
import pt.xavier.tms.vehicle.entity.ChecklistTemplate;
import pt.xavier.tms.vehicle.entity.ChecklistTemplateItem;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.VehicleAccessory;
import pt.xavier.tms.vehicle.entity.VehicleDocument;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
@Transactional
class VehicleRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_vehicle_repo_test")
            .withUsername("tms_vehicle_repo_test")
            .withPassword("tms_vehicle_repo_test");

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Autowired
    private VehicleAccessoryRepository vehicleAccessoryRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Autowired
    private ChecklistInspectionRepository checklistInspectionRepository;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    void queriesVehicleModuleRepositories() {
        Vehicle vehicle = vehicle("AA-11-BB", VehicleStatus.DISPONIVEL, "Lisboa");
        VehicleDocument validDocument = document(vehicle, VehicleDocumentType.SEGURO, DocumentStatus.VALIDO, LocalDate.now().plusDays(10));
        VehicleDocument expiredDocument = document(vehicle, VehicleDocumentType.INSPECAO, DocumentStatus.EXPIRADO, LocalDate.now().minusDays(1));
        VehicleAccessory accessory = accessory(vehicle);
        MaintenanceRecord maintenanceRecord = maintenanceRecord(vehicle);

        vehicle.getDocuments().add(validDocument);
        vehicle.getDocuments().add(expiredDocument);
        vehicle.getAccessories().add(accessory);
        vehicle.getMaintenanceRecords().add(maintenanceRecord);
        vehicleRepository.saveAndFlush(vehicle);

        ChecklistTemplate template = template("TRUCK", true);
        ChecklistTemplateItem templateItem = templateItem(template);
        template.getItems().add(templateItem);
        checklistTemplateRepository.saveAndFlush(template);

        ChecklistInspection olderInspection = inspection(vehicle, template, Instant.now().minusSeconds(3600));
        ChecklistInspection latestInspection = inspection(vehicle, template, Instant.now());
        latestInspection.getItems().add(inspectionItem(latestInspection, templateItem));
        checklistInspectionRepository.save(olderInspection);
        checklistInspectionRepository.saveAndFlush(latestInspection);

        assertThat(vehicleRepository.findByPlate("AA-11-BB")).contains(vehicle);
        assertThat(vehicleRepository.existsByPlate("AA-11-BB")).isTrue();
        assertThat(vehicleRepository.findByPlateContainingIgnoreCase("11", PageRequest.of(0, 10)).getContent())
                .extracting(Vehicle::getPlate)
                .contains("AA-11-BB");
        assertThat(vehicleRepository.findAllByFilters(VehicleStatus.DISPONIVEL, "lis", PageRequest.of(0, 10)).getContent())
                .extracting(Vehicle::getPlate)
                .contains("AA-11-BB");

        assertThat(vehicleDocumentRepository.findByVehicleId(vehicle.getId())).hasSize(2);
        assertThat(vehicleDocumentRepository.findByVehicleIdAndStatus(vehicle.getId(), DocumentStatus.EXPIRADO))
                .extracting(VehicleDocument::getDocumentType)
                .containsExactly(VehicleDocumentType.INSPECAO);
        assertThat(vehicleDocumentRepository.findByExpiryDateBetweenAndStatusNot(
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                DocumentStatus.CANCELADO
        )).extracting(VehicleDocument::getDocumentType).contains(VehicleDocumentType.SEGURO);
        assertThat(vehicleDocumentRepository.findByExpiryDateBeforeAndStatusNot(LocalDate.now(), DocumentStatus.CANCELADO))
                .extracting(VehicleDocument::getDocumentType)
                .contains(VehicleDocumentType.INSPECAO);

        assertThat(vehicleAccessoryRepository.findByVehicleId(vehicle.getId()))
                .extracting(VehicleAccessory::getAccessoryType)
                .containsExactly(AccessoryType.EXTINTOR);

        assertThat(maintenanceRepository.findByVehicleId(vehicle.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(MaintenanceRecord::getMaintenanceType)
                .containsExactly(MaintenanceType.PREVENTIVA);
        assertThat(maintenanceRepository.findByNextMaintenanceDateBetween(
                LocalDate.now().plusMonths(1),
                LocalDate.now().plusMonths(7)
        )).extracting(MaintenanceRecord::getVehicle).contains(vehicle);

        assertThat(checklistTemplateRepository.findByVehicleTypeAndIsActiveTrue("TRUCK"))
                .extracting(ChecklistTemplate::getName)
                .containsExactly("Truck inspection");
        assertThat(checklistInspectionRepository.findLatestByVehicleId(vehicle.getId()))
                .contains(latestInspection);
    }

    private static Vehicle vehicle(String plate, VehicleStatus status, String location) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(plate);
        vehicle.setBrand("Mercedes");
        vehicle.setModel("Actros");
        vehicle.setVehicleType("TRUCK");
        vehicle.setCapacity(12000);
        vehicle.setActivityLocation(location);
        vehicle.setActivityStartDate(LocalDate.now().minusYears(1));
        vehicle.setStatus(status);
        return vehicle;
    }

    private static VehicleDocument document(
            Vehicle vehicle,
            VehicleDocumentType type,
            DocumentStatus status,
            LocalDate expiryDate
    ) {
        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicle);
        document.setDocumentType(type);
        document.setStatus(status);
        document.setExpiryDate(expiryDate);
        document.setDocumentNumber(type.name() + "-001");
        return document;
    }

    private static VehicleAccessory accessory(Vehicle vehicle) {
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
        record.setDescription("Preventive maintenance");
        record.setTotalCost(BigDecimal.valueOf(100.00));
        record.setNextMaintenanceDate(LocalDate.now().plusMonths(6));
        record.setResponsibleUser("system");
        return record;
    }

    private static ChecklistTemplate template(String vehicleType, boolean active) {
        ChecklistTemplate template = new ChecklistTemplate();
        template.setVehicleType(vehicleType);
        template.setName("Truck inspection");
        template.setActive(active);
        return template;
    }

    private static ChecklistTemplateItem templateItem(ChecklistTemplate template) {
        ChecklistTemplateItem item = new ChecklistTemplateItem();
        item.setTemplate(template);
        item.setItemName("Brakes");
        item.setCritical(true);
        item.setDisplayOrder(1);
        return item;
    }

    private static ChecklistInspection inspection(Vehicle vehicle, ChecklistTemplate template, Instant performedAt) {
        ChecklistInspection inspection = new ChecklistInspection();
        inspection.setVehicle(vehicle);
        inspection.setTemplate(template);
        inspection.setPerformedBy("driver-1");
        inspection.setPerformedAt(performedAt);
        return inspection;
    }

    private static ChecklistInspectionItem inspectionItem(
            ChecklistInspection inspection,
            ChecklistTemplateItem templateItem
    ) {
        ChecklistInspectionItem item = new ChecklistInspectionItem();
        item.setInspection(inspection);
        item.setTemplateItem(templateItem);
        item.setItemName("Brakes");
        item.setCritical(true);
        item.setStatus(ChecklistItemStatus.OK);
        return item;
    }
}
