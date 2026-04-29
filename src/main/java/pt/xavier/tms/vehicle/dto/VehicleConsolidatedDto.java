package pt.xavier.tms.vehicle.dto;

import java.util.List;
import java.util.UUID;

import pt.xavier.tms.shared.enums.VehicleStatus;

public record VehicleConsolidatedDto(
        UUID id,
        String plate,
        String brand,
        String model,
        String vehicleType,
        VehicleStatus status,
        List<VehicleAccessoryDto> accessories,
        List<VehicleDocumentDto> documents,
        List<MaintenanceRecordDto> maintenanceRecords,
        List<ChecklistInspectionDto> checklistInspections,
        List<UUID> activeActivityIds,
        List<UUID> activeAlertIds
) {
}
