package pt.xavier.tms.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pt.xavier.tms.activity.entity.Activity;
import pt.xavier.tms.activity.repository.ActivityRepository;
import pt.xavier.tms.audit.entity.AuditLog;
import pt.xavier.tms.audit.repository.AuditLogRepository;
import pt.xavier.tms.shared.enums.ActivityStatus;
import pt.xavier.tms.shared.enums.AuditOperation;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
@AutoConfigureMockMvc(addFilters = false)
class AuditIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_audit_integration_test")
            .withUsername("tms_audit_integration_test")
            .withPassword("tms_audit_integration_test");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private ActivityRepository activityRepository;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createVehicleGeneratesAuditLogWithCriacaoAndVehicleEntityType() throws Exception {
        String createBody = objectMapper.writeValueAsString(new VehicleCreateRequest(
                "AA-11-BB",
                "MAN",
                "TGX",
                "CAMIAO",
                20,
                "Lisboa",
                LocalDate.of(2025, 1, 1),
                null,
                null,
                "Initial"
        ));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        List<AuditLog> logs = waitForLogs("VEHICLE");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getOperation()).isEqualTo(AuditOperation.CRIACAO);
        assertThat(logs.get(0).getEntityType()).isEqualTo("VEHICLE");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateVehicleGeneratesAuditLogWithPreviousAndNewValues() throws Exception {
        UUID vehicleId = createVehicle();

        String updateBody = objectMapper.writeValueAsString(new VehicleUpdateRequest(
                "CC-22-DD",
                "VOLVO",
                "FH",
                "CAMIAO",
                24,
                "Porto",
                LocalDate.of(2026, 1, 1),
                null,
                "Updated"
        ));

        mockMvc.perform(put("/api/v1/vehicles/{id}", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        AuditLog log = waitForLog("VEHICLE", AuditOperation.ATUALIZACAO);

        assertThat(log.getPreviousValues()).isNotNull();
        assertThat(log.getNewValues()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void activityStatusTransitionGeneratesAuditLogWithPreviousAndNewValues() throws Exception {
        UUID activityId = createActivity();

        String body = objectMapper.writeValueAsString(new StatusTransitionRequest(ActivityStatus.EM_CURSO, "go"));
        mockMvc.perform(patch("/api/v1/activities/{id}/status", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        AuditLog log = waitForLog("ACTIVITY", AuditOperation.ATUALIZACAO);

        assertThat(log.getPreviousValues()).isNotNull();
        assertThat(log.getNewValues()).isNotNull();
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "AUDITOR", "OPERADOR"})
    void auditListWithEntityTypeVehicleReturnsOnlyVehicleLogs() throws Exception {
        createVehicle();
        UUID activityId = createActivity();

        String body = objectMapper.writeValueAsString(new StatusTransitionRequest(ActivityStatus.EM_CURSO, "go"));
        mockMvc.perform(patch("/api/v1/activities/{id}/status", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit").param("entityType", "VEHICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].entityType").isArray())
                .andExpect(jsonPath("$.data.content[0].entityType").value("VEHICLE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpointDoesNotSupportMutationMethods() throws Exception {
        mockMvc.perform(post("/api/v1/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/v1/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/api/v1/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/v1/audit"))
                .andExpect(status().isMethodNotAllowed());
    }

    private UUID createVehicle() throws Exception {
        String createBody = objectMapper.writeValueAsString(new VehicleCreateRequest(
                "EE-33-FF",
                "MAN",
                "TGS",
                "CAMIAO",
                18,
                "Lisboa",
                LocalDate.of(2025, 1, 1),
                null,
                null,
                "Test"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id"));
    }

    private UUID createActivity() {
        Activity activity = new Activity();
        activity.setCode("ACT-2099-" + String.format("%04d", Math.abs((int) (System.nanoTime() % 10000))));
        activity.setTitle("Activity Audit");
        activity.setActivityType("ENTREGA");
        activity.setLocation("Lisboa");
        activity.setPlannedStart(OffsetDateTime.now().plusDays(1));
        activity.setPlannedEnd(OffsetDateTime.now().plusDays(1).plusHours(2));
        activity.setStatus(ActivityStatus.PLANEADA);
        return activityRepository.saveAndFlush(activity).getId();
    }

    private List<AuditLog> waitForLogs(String entityType) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            List<AuditLog> logs = auditLogRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("entityType"), entityType),
                    PageRequest.of(0, 50)
            ).getContent();
            if (!logs.isEmpty()) {
                return logs;
            }
            Thread.sleep(100);
        }
        Specification<AuditLog> specification = (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
        return auditLogRepository.findAll(specification, PageRequest.of(0, 50)).getContent();
    }

    private AuditLog waitForLog(String entityType, AuditOperation operation) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            List<AuditLog> logs = waitForLogs(entityType);
            for (AuditLog log : logs) {
                if (log.getOperation() == operation) {
                    return log;
                }
            }
            Thread.sleep(100);
        }
        return waitForLogs(entityType).stream()
                .filter(log -> log.getOperation() == operation)
                .findFirst()
                .orElseThrow();
    }

    private record VehicleCreateRequest(
            String plate,
            String brand,
            String model,
            String vehicleType,
            Integer capacity,
            String activityLocation,
            LocalDate activityStartDate,
            Object status,
            UUID currentDriverId,
            String notes
    ) {
    }

    private record VehicleUpdateRequest(
            String plate,
            String brand,
            String model,
            String vehicleType,
            Integer capacity,
            String activityLocation,
            LocalDate activityStartDate,
            UUID currentDriverId,
            String notes
    ) {
    }

    private record StatusTransitionRequest(ActivityStatus newStatus, String notes) {
    }
}
