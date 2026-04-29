package pt.xavier.tms.vehicle.dto;

import java.util.List;
import java.util.UUID;

public record ChecklistTemplateResponseDto(
        UUID id,
        String vehicleType,
        String name,
        boolean active,
        List<Item> items
) {

    public record Item(
            UUID id,
            String itemName,
            boolean critical,
            Integer displayOrder
    ) {
    }
}
