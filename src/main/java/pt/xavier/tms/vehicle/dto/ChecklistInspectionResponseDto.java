package pt.xavier.tms.vehicle.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pt.xavier.tms.shared.enums.ChecklistItemStatus;

public record ChecklistInspectionResponseDto(
        UUID id,
        UUID vehicleId,
        UUID activityId,
        UUID templateId,
        String performedBy,
        Instant performedAt,
        String notes,
        boolean hasCriticalFailures,
        List<Item> items
) {

    public record Item(
            UUID id,
            UUID templateItemId,
            String itemName,
            boolean critical,
            ChecklistItemStatus status,
            String notes
    ) {
    }
}
