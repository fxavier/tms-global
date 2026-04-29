package pt.xavier.tms.alert.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AlertConfigurationUpdateDto(
        @NotNull @Min(1) Integer daysBeforeWarning,
        @NotNull @Min(1) Integer daysBeforeCritical,
        @NotNull Boolean active
) {
}
