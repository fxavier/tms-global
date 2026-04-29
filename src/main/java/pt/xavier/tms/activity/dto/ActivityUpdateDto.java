package pt.xavier.tms.activity.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import pt.xavier.tms.shared.enums.ActivityPriority;

public record ActivityUpdateDto(
        String title,
        String activityType,
        String location,
        OffsetDateTime plannedStart,
        OffsetDateTime plannedEnd,
        ActivityPriority priority,
        UUID vehicleId,
        UUID driverId,
        String description,
        String notes,
        String rhOverrideJustification
) {
}
