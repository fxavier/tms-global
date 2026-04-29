package pt.xavier.tms.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import pt.xavier.tms.shared.enums.MaintenanceType;

public record MaintenanceRecordDto(
        @NotNull MaintenanceType maintenanceType,
        @NotNull LocalDate performedAt,
        Integer mileageAtService,
        @NotBlank String description,
        String supplier,
        BigDecimal totalCost,
        String partsReplaced,
        LocalDate nextMaintenanceDate,
        Integer nextMaintenanceMileage,
        @NotBlank String responsibleUser
) {
}
