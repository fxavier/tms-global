package pt.xavier.tms.driver.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.driver.dto.DriverCreateDto;
import pt.xavier.tms.driver.dto.DriverUpdateDto;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.hr.entity.Employee;
import pt.xavier.tms.hr.repository.EmployeeRepository;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.enums.DriverStatus;
import pt.xavier.tms.shared.enums.EmployeeStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Service
@ConditionalOnProperty(name = "tms.driver.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class DriverService {

    private static final String DRIVER_ENTITY_TYPE = "DRIVER";
    private static final String[] ALLOWED_DRIVER_FUNCTION_CODES = {"DRIVER", "MOTORISTA"};

    private final DriverRepository driverRepository;
    private final EmployeeRepository employeeRepository;

    public DriverService(DriverRepository driverRepository, EmployeeRepository employeeRepository) {
        this.driverRepository = driverRepository;
        this.employeeRepository = employeeRepository;
    }

    @Auditable(entityType = DRIVER_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public Driver createDriver(DriverCreateDto dto) {
        validateCreateUniqueness(dto.idNumber(), dto.licenseNumber());
        Driver driver = new Driver();
        applyCreateDto(driver, dto);
        driver.setStatus(dto.status() == null ? DriverStatus.ATIVO : dto.status());
        return driverRepository.save(driver);
    }

    @Auditable(entityType = DRIVER_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public Driver updateDriver(UUID driverId, DriverUpdateDto dto) {
        Driver driver = getDriver(driverId);
        validateUpdateUniqueness(driver, dto.idNumber(), dto.licenseNumber());
        applyUpdateDto(driver, dto);
        return driver;
    }

    @Auditable(entityType = DRIVER_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public Driver updateStatus(UUID driverId, DriverStatus status) {
        Driver driver = getDriver(driverId);
        driver.setStatus(status);
        return driver;
    }

    @Auditable(entityType = DRIVER_ENTITY_TYPE, operation = AuditOperation.ELIMINACAO)
    @Transactional
    public void deleteDriver(UUID driverId) {
        getDriver(driverId).softDelete("system");
    }

    public Driver getDriver(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver not found"));
    }

    public Page<Driver> listDrivers(DriverStatus status, String location, Pageable pageable) {
        return driverRepository.findAllByFilters(status, blankToNull(location), pageable);
    }

    private void validateCreateUniqueness(String idNumber, String licenseNumber) {
        if (driverRepository.existsByIdNumber(idNumber)) {
            throw new BusinessException("DUPLICATE_DRIVER_ID_NUMBER", "Driver id number already exists");
        }
        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BusinessException("DUPLICATE_DRIVER_LICENSE_NUMBER", "Driver license number already exists");
        }
    }

    private void validateUpdateUniqueness(Driver driver, String idNumber, String licenseNumber) {
        driverRepository.findByIdNumber(idNumber)
                .filter(existing -> !existing.getId().equals(driver.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("DUPLICATE_DRIVER_ID_NUMBER", "Driver id number already exists");
                });

        if (!driver.getLicenseNumber().equals(licenseNumber) && driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BusinessException("DUPLICATE_DRIVER_LICENSE_NUMBER", "Driver license number already exists");
        }
    }

    private void applyCreateDto(Driver driver, DriverCreateDto dto) {
        driver.setFullName(dto.fullName());
        driver.setPhone(dto.phone());
        driver.setAddress(dto.address());
        driver.setIdNumber(dto.idNumber());
        driver.setLicenseNumber(dto.licenseNumber());
        driver.setLicenseCategory(dto.licenseCategory());
        driver.setLicenseIssueDate(dto.licenseIssueDate());
        driver.setLicenseExpiryDate(dto.licenseExpiryDate());
        driver.setActivityLocation(dto.activityLocation());
        driver.setEmployee(resolveAndValidateEmployee(dto.employeeId()));
        driver.setNotes(dto.notes());
    }

    private void applyUpdateDto(Driver driver, DriverUpdateDto dto) {
        driver.setFullName(dto.fullName());
        driver.setPhone(dto.phone());
        driver.setAddress(dto.address());
        driver.setIdNumber(dto.idNumber());
        driver.setLicenseNumber(dto.licenseNumber());
        driver.setLicenseCategory(dto.licenseCategory());
        driver.setLicenseIssueDate(dto.licenseIssueDate());
        driver.setLicenseExpiryDate(dto.licenseExpiryDate());
        driver.setActivityLocation(dto.activityLocation());
        driver.setEmployee(resolveAndValidateEmployee(dto.employeeId()));
        driver.setNotes(dto.notes());
    }

    private Employee resolveAndValidateEmployee(UUID employeeId) {
        if (employeeId == null) {
            return null;
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("EMPLOYEE_NOT_FOUND", "Employee not found"));
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("EMPLOYEE_NOT_ACTIVE", "Associated employee must be ACTIVE");
        }
        if (employee.getFunction() != null && employee.getFunction().getCode() != null) {
            String normalizedCode = employee.getFunction().getCode().trim().toUpperCase();
            boolean allowed = false;
            for (String code : ALLOWED_DRIVER_FUNCTION_CODES) {
                if (code.equals(normalizedCode)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new BusinessException(
                        "EMPLOYEE_FUNCTION_NOT_ALLOWED_FOR_DRIVER",
                        "Associated employee function is not allowed for driver"
                );
            }
        }
        return employee;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
