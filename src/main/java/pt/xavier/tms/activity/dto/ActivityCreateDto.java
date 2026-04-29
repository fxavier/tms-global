package pt.xavier.tms.activity.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.xavier.tms.shared.enums.ActivityPriority;

public record ActivityCreateDto(
        @NotBlank String title,
        @NotBlank String activityType,
        @NotBlank String location,
        @NotNull OffsetDateTime plannedStart,
        @NotNull OffsetDateTime plannedEnd,
        ActivityPriority priority,
        UUID vehicleId,
        UUID driverId,
        String description,
        String notes,
        String rhOverrideJustification
) {
}
