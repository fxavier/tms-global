package pt.xavier.tms.driver.dto;

import java.time.LocalDate;
import java.util.UUID;

import pt.xavier.tms.shared.enums.DriverStatus;

public record DriverResponseDto(
        UUID id,
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
