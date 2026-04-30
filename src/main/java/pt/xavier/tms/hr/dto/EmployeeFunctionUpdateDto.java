package pt.xavier.tms.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeFunctionUpdateDto(
        @NotBlank @Size(max = 150) String name,
        String description
) {
}
