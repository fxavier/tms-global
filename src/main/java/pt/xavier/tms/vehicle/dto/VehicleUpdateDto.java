package pt.xavier.tms.vehicle.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VehicleUpdateDto(
        @NotBlank String plate,
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String vehicleType,
        @NotNull @Positive Integer capacity,
        @NotBlank String activityLocation,
        @NotNull LocalDate activityStartDate,
        UUID currentDriverId,
        String notes
) {
}
