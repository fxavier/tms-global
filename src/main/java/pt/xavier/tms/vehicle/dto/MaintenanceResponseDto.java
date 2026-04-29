package pt.xavier.tms.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import pt.xavier.tms.shared.enums.MaintenanceType;

public record MaintenanceResponseDto(
        UUID id,
        UUID vehicleId,
        MaintenanceType maintenanceType,
        LocalDate performedAt,
        Integer mileageAtService,
        String description,
        String supplier,
        BigDecimal totalCost,
        String partsReplaced,
        LocalDate nextMaintenanceDate,
        Integer nextMaintenanceMileage,
        String responsibleUser
) {
}
