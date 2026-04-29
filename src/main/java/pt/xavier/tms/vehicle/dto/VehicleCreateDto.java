package pt.xavier.tms.vehicle.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import pt.xavier.tms.shared.enums.VehicleStatus;

public record VehicleCreateDto(
        @NotBlank String plate,
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String vehicleType,
        @NotNull @Positive Integer capacity,
        @NotBlank String activityLocation,
        @NotNull LocalDate activityStartDate,
        VehicleStatus status,
        UUID currentDriverId,
        String notes
) {
}
