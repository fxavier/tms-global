package pt.xavier.tms.driver.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.shared.enums.DriverStatus;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class DriverEndpointsIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_driver_endpoint_test")
            .withUsername("tms_driver_endpoint_test")
            .withPassword("tms_driver_endpoint_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DriverRepository driverRepository;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void driverCrudWorksViaApi() throws Exception {
        String createBody = objectMapper.writeValueAsString(new DriverCreateRequest(
                "Joao Silva",
                "+351910000000",
                "Rua Central",
                "12345678",
                "L-12345",
                "C",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2028, 1, 1),
                "Lisboa",
                null,
                DriverStatus.ATIVO,
                "Initial"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.fullName").value("Joao Silva"))
                .andReturn();

        UUID driverId = UUID.fromString(JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/v1/drivers/{id}", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(driverId.toString()))
                .andExpect(jsonPath("$.data.idNumber").value("12345678"));

        String updateBody = objectMapper.writeValueAsString(new DriverUpdateRequest(
                "Joao Silva Updated",
                "+351910000001",
                "Rua Nova",
                "12345678",
                "L-12345",
                "CE",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2028, 1, 1),
                "Porto",
                null,
                "Updated"
        ));

        mockMvc.perform(put("/api/v1/drivers/{id}", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Joao Silva Updated"))
                .andExpect(jsonPath("$.data.activityLocation").value("Porto"));

        String statusBody = objectMapper.writeValueAsString(new DriverStatusRequest(DriverStatus.SUSPENSO));
        mockMvc.perform(patch("/api/v1/drivers/{id}/status", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENSO"));

        mockMvc.perform(delete("/api/v1/drivers/{id}", driverId))
                .andExpect(status().isOk());

        assertNotNull(driverRepository.findById(driverId).orElseThrow().getDeletedAt());
    }

    private UUID createDriverForRead() throws Exception {
        Driver driver = new Driver();
        driver.setFullName("Maria Silva");
        driver.setPhone("+351910000100");
        driver.setAddress("Rua Azul");
        driver.setIdNumber(UUID.randomUUID().toString());
        driver.setLicenseNumber("L-" + UUID.randomUUID());
        driver.setLicenseCategory("C");
        driver.setLicenseIssueDate(LocalDate.of(2024, 1, 1));
        driver.setLicenseExpiryDate(LocalDate.of(2029, 1, 1));
        driver.setActivityLocation("Braga");
        driver.setStatus(DriverStatus.ATIVO);
        return driverRepository.saveAndFlush(driver).getId();
    }

    private record DriverCreateRequest(
            String fullName,
            String phone,
            String address,
            String idNumber,
            String licenseNumber,
            String licenseCategory,
            LocalDate licenseIssueDate,
            LocalDate licenseExpiryDate,
            String activityLocation,
            UUID employeeId,
            DriverStatus status,
            String notes
    ) {
    }

    private record DriverUpdateRequest(
            String fullName,
            String phone,
            String address,
            String idNumber,
            String licenseNumber,
            String licenseCategory,
            LocalDate licenseIssueDate,
            LocalDate licenseExpiryDate,
            String activityLocation,
            UUID employeeId,
            String notes
    ) {
    }

    private record DriverStatusRequest(DriverStatus status) {
    }
}
