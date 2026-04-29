package pt.xavier.tms.driver.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

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

import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;
import pt.xavier.tms.shared.enums.DriverStatus;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/tms",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/tms/protocol/openid-connect/certs"
})
class DriverJpaMappingIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_driver_test")
            .withUsername("tms_driver_test")
            .withPassword("tms_driver_test");

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
    void persistsDriverEntitiesAgainstFlywaySchema() {
        UUID fileId = UUID.randomUUID();
        insertFileRecord(fileId);

        Driver driver = driver();
        DriverDocument document = driverDocument(driver, fileId);
        driver.getDocuments().add(document);

        entityManager.persist(driver);
        entityManager.flush();
        entityManager.clear();

        Driver persisted = entityManager.find(Driver.class, driver.getId());

        assertThat(persisted).isNotNull();
        assertThat(persisted.getCreatedBy()).isEqualTo("system");
        assertThat(persisted.getDocuments()).hasSize(1);
        assertThat(persisted.getDocuments().getFirst().getDocumentType()).isEqualTo(DriverDocumentType.CARTA_CONDUCAO);
    }

    private static Driver driver() {
        Driver driver = new Driver();
        driver.setFullName("Joao Silva");
        driver.setPhone("+351910000000");
        driver.setAddress("Rua Central");
        driver.setIdNumber("12345678");
        driver.setLicenseNumber("L-12345");
        driver.setLicenseCategory("C");
        driver.setLicenseIssueDate(LocalDate.now().minusYears(2));
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        driver.setActivityLocation("Lisboa");
        driver.setStatus(DriverStatus.ATIVO);
        return driver;
    }

    private void insertFileRecord(UUID fileId) {
        entityManager.createNativeQuery("""
                insert into files (
                    id,
                    original_filename,
                    storage_key,
                    content_type,
                    size_bytes,
                    uploaded_by,
                    uploaded_at
                ) values (
                    :id,
                    :originalFilename,
                    :storageKey,
                    :contentType,
                    :sizeBytes,
                    :uploadedBy,
                    now()
                )
                """)
                .setParameter("id", fileId)
                .setParameter("originalFilename", "license.pdf")
                .setParameter("storageKey", "driver-documents/license.pdf")
                .setParameter("contentType", "application/pdf")
                .setParameter("sizeBytes", 2048L)
                .setParameter("uploadedBy", "system")
                .executeUpdate();
    }

    private static DriverDocument driverDocument(Driver driver, UUID fileId) {
        DriverDocument document = new DriverDocument();
        document.setDriver(driver);
        document.setDocumentType(DriverDocumentType.CARTA_CONDUCAO);
        document.setDocumentNumber("C-9988");
        document.setIssueDate(LocalDate.now().minusYears(2));
        document.setExpiryDate(LocalDate.now().plusYears(3));
        document.setIssuingEntity("IMT");
        document.setCategory("C");
        document.setStatus(DocumentStatus.VALIDO);
        document.setFileId(fileId);
        return document;
    }
}
