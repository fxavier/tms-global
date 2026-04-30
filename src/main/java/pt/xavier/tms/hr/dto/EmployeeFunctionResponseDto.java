package pt.xavier.tms.hr.dto;

import java.util.UUID;

public record EmployeeFunctionResponseDto(
        UUID id,
        String code,
        String name,
        String description,
        boolean isActive
) {
}
