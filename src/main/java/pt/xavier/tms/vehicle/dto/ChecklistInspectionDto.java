package pt.xavier.tms.vehicle.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import pt.xavier.tms.shared.enums.ChecklistItemStatus;

public record ChecklistInspectionDto(
        UUID activityId,
        @NotNull UUID templateId,
        @NotBlank String performedBy,
        Instant performedAt,
        String notes,
        @Valid @NotEmpty List<Item> items
) {

    public record Item(
            UUID templateItemId,
            @NotBlank String itemName,
            boolean critical,
            @NotNull ChecklistItemStatus status,
            String notes
    ) {
    }
}
