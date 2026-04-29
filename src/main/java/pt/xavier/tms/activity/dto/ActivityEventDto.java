package pt.xavier.tms.activity.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import pt.xavier.tms.shared.enums.ActivityStatus;

public record ActivityEventDto(
        UUID id,
        String eventType,
        ActivityStatus previousStatus,
        ActivityStatus newStatus,
        String performedBy,
        OffsetDateTime performedAt,
        String notes
) {
}
