package pt.xavier.tms.driver.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverUpdateDto(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        String address,
        @NotBlank @Size(max = 50) String idNumber,
        @NotBlank @Size(max = 50) String licenseNumber,
        @Size(max = 30) String licenseCategory,
        LocalDate licenseIssueDate,
        LocalDate licenseExpiryDate,
        @Size(max = 200) String activityLocation,
        String notes
) {
}
