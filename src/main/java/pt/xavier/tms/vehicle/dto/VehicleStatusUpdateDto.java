package pt.xavier.tms.vehicle.dto;

import jakarta.validation.constraints.NotNull;

import pt.xavier.tms.shared.enums.VehicleStatus;

public record VehicleStatusUpdateDto(@NotNull VehicleStatus status) {
}
