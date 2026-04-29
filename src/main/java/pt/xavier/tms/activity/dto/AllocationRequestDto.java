package pt.xavier.tms.activity.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AllocationRequestDto(
        @NotNull UUID vehicleId,
        @NotNull UUID driverId,
        String rhOverrideJustification
) {
}
