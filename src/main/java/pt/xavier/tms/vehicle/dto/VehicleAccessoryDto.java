package pt.xavier.tms.vehicle.dto;

import java.time.Instant;
import java.util.UUID;

import pt.xavier.tms.shared.enums.AccessoryStatus;
import pt.xavier.tms.shared.enums.AccessoryType;

public record VehicleAccessoryDto(
        UUID id,
        AccessoryType accessoryType,
        AccessoryStatus status,
        Instant lastCheckedAt,
        String lastCheckedBy,
        String notes
) {
}
