package pt.xavier.tms.driver.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.driver.dto.DriverCreateDto;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.hr.repository.EmployeeRepository;
import pt.xavier.tms.shared.enums.DriverStatus;
import pt.xavier.tms.shared.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class DriverServiceTests {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private EmployeeRepository employeeRepository;

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
                null,
                DriverStatus.ATIVO,
                null
        );
    }
}
