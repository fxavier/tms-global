package pt.xavier.tms.activity.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import pt.xavier.tms.shared.enums.ActivityPriority;
import pt.xavier.tms.shared.enums.ActivityStatus;

public record ActivityResponseDto(
        UUID id,
        String code,
        String title,
        String activityType,
        String location,
        OffsetDateTime plannedStart,
        OffsetDateTime plannedEnd,
        OffsetDateTime actualStart,
        OffsetDateTime actualEnd,
        ActivityPriority priority,
        ActivityStatus status,
        UUID vehicleId,
        UUID driverId,
        String description,
        String notes,
        String rhOverrideJustification
) {
}
