package pt.xavier.tms.vehicle.dto;

import java.time.LocalDate;
import java.util.UUID;

import pt.xavier.tms.shared.enums.VehicleStatus;

public record VehicleResponseDto(
        UUID id,
        String plate,
        String brand,
        String model,
        String vehicleType,
        Integer capacity,
        String activityLocation,
        LocalDate activityStartDate,
        VehicleStatus status,
        UUID currentDriverId,
        String notes
) {
}
