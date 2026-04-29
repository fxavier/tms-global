package pt.xavier.tms.vehicle.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.mock.web.MockMultipartFile;

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
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.vehicle.repository.ChecklistTemplateRepository;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class VehicleEndpointsIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_vehicle_endpoint_test")
            .withUsername("tms_vehicle_endpoint_test")
            .withPassword("tms_vehicle_endpoint_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Autowired
    private ChecklistInspectionRepository checklistInspectionRepository;

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
    @WithMockUser(roles = "GESTOR_FROTA")
    void createVehicleReturnsCreatedPayload() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new VehicleCreateRequest(
                "AA-11-BB",
                "Mercedes",
                "Actros",
                "TRUCK",
                12000,
                "Lisboa",
                LocalDate.of(2025, 1, 1),
                VehicleStatus.DISPONIVEL,
                null,
                "Ready"
        ));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.plate").value("AA-11-BB"))
                .andExpect(jsonPath("$.data.brand").value("Mercedes"));
    }

    @Test
    @WithMockUser(roles = "GESTOR_FROTA")
    void createVehicleWithDuplicatePlateReturnsUnprocessableEntity() throws Exception {
        vehicleRepository.saveAndFlush(vehicle("AA-11-BB", VehicleStatus.DISPONIVEL, "Lisboa"));

        String requestBody = objectMapper.writeValueAsString(new VehicleCreateRequest(
                "AA-11-BB",
                "Volvo",
                "FH",
                "TRUCK",
                14000,
                "Porto",
                LocalDate.of(2025, 2, 1),
                VehicleStatus.DISPONIVEL,
                null,
                null
        ));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_VEHICLE_PLATE"));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void searchVehiclesReturnsPagedResults() throws Exception {
        vehicleRepository.saveAndFlush(vehicle("AA-11-BB", VehicleStatus.DISPONIVEL, "Lisboa"));
        vehicleRepository.saveAndFlush(vehicle("ZZ-99-ZZ", VehicleStatus.DISPONIVEL, "Porto"));

        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("q", "AA")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].plate").value("AA-11-BB"))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void consolidatedVehicleEndpointReturnsAggregatedStructure() throws Exception {
        Vehicle vehicle = vehicle("AA-11-BB", VehicleStatus.DISPONIVEL, "Lisboa");
        VehicleDocument document = document(vehicle);
        VehicleAccessory accessory = accessory(vehicle);
        MaintenanceRecord maintenanceRecord = maintenanceRecord(vehicle);

        vehicle.getDocuments().add(document);
        vehicle.getAccessories().add(accessory);
        vehicle.getMaintenanceRecords().add(maintenanceRecord);
        vehicleRepository.saveAndFlush(vehicle);

        ChecklistTemplate template = template("TRUCK", true);
        ChecklistTemplateItem templateItem = templateItem(template);
        template.getItems().add(templateItem);
        checklistTemplateRepository.saveAndFlush(template);

        ChecklistInspection inspection = inspection(vehicle, template);
        inspection.getItems().add(inspectionItem(inspection, templateItem));
        checklistInspectionRepository.saveAndFlush(inspection);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/vehicles/{id}/consolidated", vehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(vehicle.getId().toString()))
                .andExpect(jsonPath("$.data.documents.length()").value(1))
                .andExpect(jsonPath("$.data.accessories.length()").value(1))
                .andExpect(jsonPath("$.data.maintenanceRecords.length()").value(1))
                .andExpect(jsonPath("$.data.checklistInspections.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "GESTOR_FROTA")
    void createAndGetVehicleWorksViaApi() throws Exception {
        String createBody = objectMapper.writeValueAsString(new VehicleCreateRequest(
                "CR-11-UD",
                "MAN",
                "TGX",
                "TRUCK",
                15000,
                "Coimbra",
                LocalDate.of(2025, 3, 1),
                VehicleStatus.DISPONIVEL,
                null,
                "Created by test"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        UUID vehicleId = UUID.fromString(JsonPath.read(response, "$.data.id"));

        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plate").value("CR-11-UD"));

    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void uploadPdfWithinLimitWorks() throws Exception {
        byte[] content = "pdf-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", content);

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.originalFilename").value("doc.pdf"))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"));
    }

    private static Vehicle vehicle(String plate, VehicleStatus status, String location) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(plate);
        vehicle.setBrand("Mercedes");
        vehicle.setModel("Actros");
        vehicle.setVehicleType("TRUCK");
        vehicle.setCapacity(12000);
        vehicle.setActivityLocation(location);
        vehicle.setActivityStartDate(LocalDate.now().minusDays(5));
        vehicle.setStatus(status);
        return vehicle;
    }

    private static VehicleDocument document(Vehicle vehicle) {
        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicle);
        document.setDocumentType(VehicleDocumentType.SEGURO);
        document.setDocumentNumber("SEG-001");
        document.setIssueDate(LocalDate.now().minusMonths(1));
        document.setExpiryDate(LocalDate.now().plusMonths(11));
        document.setStatus(DocumentStatus.VALIDO);
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
        record.setDescription("Preventive");
        record.setTotalCost(BigDecimal.valueOf(100));
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

    private static ChecklistInspection inspection(Vehicle vehicle, ChecklistTemplate template) {
        ChecklistInspection inspection = new ChecklistInspection();
        inspection.setVehicle(vehicle);
        inspection.setTemplate(template);
        inspection.setPerformedBy("driver-1");
        inspection.setPerformedAt(Instant.now());
        return inspection;
    }

    private static ChecklistInspectionItem inspectionItem(ChecklistInspection inspection, ChecklistTemplateItem templateItem) {
        ChecklistInspectionItem item = new ChecklistInspectionItem();
        item.setInspection(inspection);
        item.setTemplateItem(templateItem);
        item.setItemName("Brakes");
        item.setCritical(true);
        item.setStatus(ChecklistItemStatus.OK);
        return item;
    }

    private record VehicleCreateRequest(
            String plate,
            String brand,
            String model,
            String vehicleType,
            Integer capacity,
            String activityLocation,
            LocalDate activityStartDate,
            VehicleStatus status,
            java.util.UUID currentDriverId,
            String notes
    ) {
    }

}
