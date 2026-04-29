package pt.xavier.tms.driver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.driver.dto.DriverCreateDto;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.integration.exception.RhIntegrationException;
import pt.xavier.tms.integration.port.RhIntegrationPort;
import pt.xavier.tms.shared.enums.DriverStatus;
import pt.xavier.tms.shared.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class DriverServiceTests {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private RhIntegrationPort rhIntegrationPort;

    @InjectMocks
    private DriverService driverService;

    @Test
    void createDriverRejectsDuplicateIdNumber() {
        DriverCreateDto dto = createDto("12345678", "L-1234");
        when(driverRepository.existsByIdNumber(dto.idNumber())).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Driver id number already exists");
    }

    @Test
    void getAvailabilityReturnsUnavailableWhenRhReturnsUnavailable() {
        UUID driverId = UUID.randomUUID();
        Driver driver = existingDriver(driverId);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);

        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rhIntegrationPort.checkAvailability(driverId, startDate, endDate))
                .thenReturn(new DriverAvailabilityDto(
                        driverId,
                        false,
                        "DRIVER_ON_LEAVE",
                        List.of()
                ));

        DriverAvailabilityDto result = driverService.getAvailability(driverId, startDate, endDate);

        assertThat(result.available()).isFalse();
        assertThat(result.reason()).isEqualTo("DRIVER_ON_LEAVE");
    }

    @Test
    void getAvailabilityReturnsFallbackWhenRhIntegrationFails() {
        UUID driverId = UUID.randomUUID();
        Driver driver = existingDriver(driverId);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);

        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rhIntegrationPort.checkAvailability(driverId, startDate, endDate))
                .thenThrow(new RhIntegrationException("RH unavailable", new RuntimeException("timeout")));

        DriverAvailabilityDto result = driverService.getAvailability(driverId, startDate, endDate);

        assertThat(result.driverId()).isEqualTo(driverId);
        assertThat(result.available()).isFalse();
        assertThat(result.reason()).isEqualTo("RH_SYSTEM_UNAVAILABLE");
        assertThat(result.absences()).isEmpty();
    }

    private static DriverCreateDto createDto(String idNumber, String licenseNumber) {
        return new DriverCreateDto(
                "Joao Silva",
                "+351910000000",
                "Rua Central",
                idNumber,
                licenseNumber,
                "C",
                LocalDate.now().minusYears(2),
                LocalDate.now().plusYears(3),
                "Lisboa",
                DriverStatus.ATIVO,
                null
        );
    }

    private static Driver existingDriver(UUID id) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setFullName("Joao Silva");
        driver.setIdNumber("12345678");
        driver.setLicenseNumber("L-1234");
        return driver;
    }
}
