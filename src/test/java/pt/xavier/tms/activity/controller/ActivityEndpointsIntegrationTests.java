package pt.xavier.tms.activity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import pt.xavier.tms.activity.entity.Activity;
import pt.xavier.tms.activity.repository.ActivityRepository;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.integration.port.RhIntegrationPort;
import pt.xavier.tms.shared.enums.ActivityStatus;
import pt.xavier.tms.shared.enums.DriverStatus;
import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ActivityEndpointsIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_activity_endpoint_test")
            .withUsername("tms_activity_endpoint_test")
            .withPassword("tms_activity_endpoint_test");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private DriverRepository driverRepository;

    @MockBean
    private RhIntegrationPort rhIntegrationPort;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void createActivityGeneratesUniqueCode() throws Exception {
        String request = objectMapper.writeValueAsString(new ActivityCreateRequest(
                "Entrega 1",
                "ENTREGA",
                "Lisboa",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                null,
                null,
                null,
                null,
                null,
                null
        ));

        MvcResult first = mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.matchesRegex("ACT-\\d{4}-\\d{4}")))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.matchesRegex("ACT-\\d{4}-\\d{4}")))
                .andReturn();

        String firstCode = JsonPath.read(first.getResponse().getContentAsString(), "$.data.code");
        String secondCode = JsonPath.read(second.getResponse().getContentAsString(), "$.data.code");
        org.assertj.core.api.Assertions.assertThat(secondCode).isNotEqualTo(firstCode);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void allocateWithVehicleInMaintenanceReturns422() throws Exception {
        when(rhIntegrationPort.checkAvailability(any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new DriverAvailabilityDto(UUID.randomUUID(), true, null, List.of()));

        UUID vehicleId = createVehicle(VehicleStatus.EM_MANUTENCAO);
        UUID driverId = createDriver(DriverStatus.ATIVO);
        UUID activityId = createActivity();

        String request = objectMapper.writeValueAsString(new AllocationRequest(vehicleId, driverId, null));

        mockMvc.perform(post("/api/v1/activities/{id}/allocate", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ALLOCATION_BLOCKED"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void invalidStatusTransitionReturns422() throws Exception {
        UUID activityId = createActivity(ActivityStatus.CONCLUIDA);

        String invalid = objectMapper.writeValueAsString(new StatusTransitionRequest(ActivityStatus.EM_CURSO, "invalid"));
        mockMvc.perform(patch("/api/v1/activities/{id}/status", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    private UUID createActivity() {
        return createActivity(ActivityStatus.PLANEADA);
    }

    private UUID createActivity(ActivityStatus status) {
        Activity activity = new Activity();
        activity.setCode("ACT-2099-" + String.format("%04d", Math.abs((int) (System.nanoTime() % 10000))));
        activity.setTitle("Activity Test");
        activity.setActivityType("ENTREGA");
        activity.setLocation("Lisboa");
        activity.setPlannedStart(OffsetDateTime.now().plusDays(1));
        activity.setPlannedEnd(OffsetDateTime.now().plusDays(1).plusHours(2));
        activity.setStatus(status);
        return activityRepository.saveAndFlush(activity).getId();
    }

    private UUID createVehicle(VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate("AA-" + Math.abs((int) (System.nanoTime() % 10000)));
        vehicle.setBrand("MAN");
        vehicle.setModel("TGX");
        vehicle.setVehicleType("CAMIAO");
        vehicle.setCapacity(20);
        vehicle.setActivityLocation("Lisboa");
        vehicle.setActivityStartDate(LocalDate.of(2025, 1, 1));
        vehicle.setStatus(status);
        return vehicleRepository.saveAndFlush(vehicle).getId();
    }

    private UUID createDriver(DriverStatus status) {
        Driver driver = new Driver();
        driver.setFullName("Driver Test");
        driver.setPhone("+351910000000");
        driver.setAddress("Rua");
        driver.setIdNumber(UUID.randomUUID().toString());
        driver.setLicenseNumber("L-" + UUID.randomUUID());
        driver.setLicenseCategory("C");
        driver.setLicenseIssueDate(LocalDate.of(2023, 1, 1));
        driver.setLicenseExpiryDate(LocalDate.of(2030, 1, 1));
        driver.setActivityLocation("Lisboa");
        driver.setStatus(status);
        return driverRepository.saveAndFlush(driver).getId();
    }

    private record ActivityCreateRequest(
            String title,
            String activityType,
            String location,
            OffsetDateTime plannedStart,
            OffsetDateTime plannedEnd,
            Object priority,
            UUID vehicleId,
            UUID driverId,
            String description,
            String notes,
            String rhOverrideJustification
    ) {
    }

    private record AllocationRequest(UUID vehicleId, UUID driverId, String rhOverrideJustification) {
    }

    private record StatusTransitionRequest(ActivityStatus newStatus, String notes) {
    }
}
