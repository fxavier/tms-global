package pt.xavier.tms.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateDto(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 50) String phone,
        @Size(max = 150) String email,
        @Size(max = 100) String idNumber,
        UUID functionId,
        LocalDate hireDate,
        LocalDate terminationDate,
        BigDecimal baseSalary,
        @Size(max = 3) String currency,
        String notes
) {
}
