package pt.xavier.tms.alert.dto;

import java.util.UUID;

import pt.xavier.tms.shared.enums.AlertType;

public record AlertConfigurationResponseDto(
        UUID id,
        AlertType alertType,
        String entityType,
        Integer daysBeforeWarning,
        Integer daysBeforeCritical,
        boolean active
) {
}
