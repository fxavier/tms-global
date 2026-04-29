package pt.xavier.tms.driver.dto;

import jakarta.validation.constraints.NotNull;

import pt.xavier.tms.shared.enums.DriverStatus;

public record DriverStatusUpdateDto(
        @NotNull DriverStatus status
) {
}
