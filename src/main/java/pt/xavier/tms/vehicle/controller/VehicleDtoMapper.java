package pt.xavier.tms.vehicle.controller;

import pt.xavier.tms.vehicle.dto.ChecklistInspectionResponseDto;
import pt.xavier.tms.vehicle.dto.ChecklistTemplateResponseDto;
import pt.xavier.tms.vehicle.dto.FileResponseDto;
import pt.xavier.tms.vehicle.dto.MaintenanceResponseDto;
import pt.xavier.tms.vehicle.dto.VehicleDocumentResponseDto;
import pt.xavier.tms.vehicle.dto.VehicleResponseDto;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.entity.ChecklistTemplate;
import pt.xavier.tms.vehicle.entity.FileRecord;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.VehicleDocument;

final class VehicleDtoMapper {

    private VehicleDtoMapper() {
    }

    static VehicleResponseDto toVehicleResponse(Vehicle vehicle) {
        return new VehicleResponseDto(
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVehicleType(),
                vehicle.getCapacity(),
                vehicle.getActivityLocation(),
                vehicle.getActivityStartDate(),
                vehicle.getStatus(),
                vehicle.getCurrentDriverId(),
                vehicle.getNotes()
        );
    }

    static VehicleDocumentResponseDto toDocumentResponse(VehicleDocument document) {
        return new VehicleDocumentResponseDto(
                document.getId(),
                document.getVehicle().getId(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getIssuingEntity(),
                document.getStatus(),
                document.getNotes(),
                document.getFile() == null ? null : document.getFile().getId()
        );
    }

    static MaintenanceResponseDto toMaintenanceResponse(MaintenanceRecord record) {
        return new MaintenanceResponseDto(
                record.getId(),
                record.getVehicle().getId(),
                record.getMaintenanceType(),
                record.getPerformedAt(),
                record.getMileageAtService(),
                record.getDescription(),
                record.getSupplier(),
                record.getTotalCost(),
                record.getPartsReplaced(),
                record.getNextMaintenanceDate(),
                record.getNextMaintenanceMileage(),
                record.getResponsibleUser()
        );
    }

    static ChecklistTemplateResponseDto toTemplateResponse(ChecklistTemplate template) {
        return new ChecklistTemplateResponseDto(
                template.getId(),
                template.getVehicleType(),
                template.getName(),
                template.isActive(),
                template.getItems().stream()
                        .map(item -> new ChecklistTemplateResponseDto.Item(
                                item.getId(),
                                item.getItemName(),
                                item.isCritical(),
                                item.getDisplayOrder()
                        ))
                        .toList()
        );
    }

    static ChecklistInspectionResponseDto toChecklistResponse(ChecklistInspection inspection) {
        return new ChecklistInspectionResponseDto(
                inspection.getId(),
                inspection.getVehicle().getId(),
                inspection.getActivityId(),
                inspection.getTemplate().getId(),
                inspection.getPerformedBy(),
                inspection.getPerformedAt(),
                inspection.getNotes(),
                inspection.hasCriticalFailures(),
                inspection.getItems().stream()
                        .map(item -> new ChecklistInspectionResponseDto.Item(
                                item.getId(),
                                item.getTemplateItem() == null ? null : item.getTemplateItem().getId(),
                                item.getItemName(),
                                item.isCritical(),
                                item.getStatus(),
                                item.getNotes()
                        ))
                        .toList()
        );
    }

    static FileResponseDto toFileResponse(FileRecord file) {
        return new FileResponseDto(
                file.getId(),
                file.getOriginalFilename(),
                file.getStorageKey(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getUploadedBy(),
                file.getUploadedAt()
        );
    }
}
