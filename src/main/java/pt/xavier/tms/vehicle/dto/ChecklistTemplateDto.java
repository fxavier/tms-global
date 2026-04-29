package pt.xavier.tms.vehicle.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ChecklistTemplateDto(
        @NotBlank String vehicleType,
        @NotBlank String name,
        @NotNull Boolean active,
        @Valid @NotEmpty List<Item> items
) {

    public record Item(
            @NotBlank String itemName,
            boolean critical,
            @NotNull Integer displayOrder
    ) {
    }
}
