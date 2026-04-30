package pt.xavier.tms.driver.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import pt.xavier.tms.shared.enums.DriverStatus;

public record DriverCreateDto(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        String address,
        @NotBlank @Size(max = 50) String idNumber,
        @NotBlank @Size(max = 50) String licenseNumber,
        @Size(max = 30) String licenseCategory,
        LocalDate licenseIssueDate,
        LocalDate licenseExpiryDate,
        @Size(max = 200) String activityLocation,
        UUID employeeId,
        DriverStatus status,
        String notes
) {
}
