package pt.xavier.tms.vehicle.service;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.vehicle.dto.ChecklistInspectionDto;
import pt.xavier.tms.vehicle.dto.MaintenanceRecordDto;
import pt.xavier.tms.vehicle.dto.VehicleConsolidatedDto;
import pt.xavier.tms.vehicle.dto.VehicleCreateDto;
import pt.xavier.tms.vehicle.dto.VehicleDocumentDto;
import pt.xavier.tms.vehicle.dto.VehicleUpdateDto;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.VehicleDocument;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@Service
@ConditionalOnProperty(name = "tms.vehicle.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class VehicleService {

    private static final String VEHICLE_ENTITY_TYPE = "VEHICLE";

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Auditable(entityType = VEHICLE_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public Vehicle createVehicle(VehicleCreateDto dto) {
        if (vehicleRepository.existsByPlate(dto.plate())) {
            throw new BusinessException("DUPLICATE_VEHICLE_PLATE", "Vehicle plate already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(dto.plate());
        vehicle.setBrand(dto.brand());
        vehicle.setModel(dto.model());
        vehicle.setVehicleType(dto.vehicleType());
        vehicle.setCapacity(dto.capacity());
        vehicle.setActivityLocation(dto.activityLocation());
        vehicle.setActivityStartDate(dto.activityStartDate());
        vehicle.setStatus(dto.status() == null ? VehicleStatus.DISPONIVEL : dto.status());
        vehicle.setCurrentDriverId(dto.currentDriverId());
        vehicle.setNotes(dto.notes());
        return vehicleRepository.save(vehicle);
    }

    @Auditable(entityType = VEHICLE_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public Vehicle updateVehicle(UUID vehicleId, VehicleUpdateDto dto) {
        Vehicle vehicle = getVehicle(vehicleId);
        vehicleRepository.findByPlate(dto.plate())
                .filter(existing -> !existing.getId().equals(vehicleId))
                .ifPresent(existing -> {
                    throw new BusinessException("DUPLICATE_VEHICLE_PLATE", "Vehicle plate already exists");
                });

        vehicle.setPlate(dto.plate());
        vehicle.setBrand(dto.brand());
        vehicle.setModel(dto.model());
        vehicle.setVehicleType(dto.vehicleType());
        vehicle.setCapacity(dto.capacity());
        vehicle.setActivityLocation(dto.activityLocation());
        vehicle.setActivityStartDate(dto.activityStartDate());
        vehicle.setCurrentDriverId(dto.currentDriverId());
        vehicle.setNotes(dto.notes());
        return vehicle;
    }

    @Auditable(entityType = VEHICLE_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public Vehicle updateStatus(UUID vehicleId, VehicleStatus status) {
        Vehicle vehicle = getVehicle(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.ABATIDA && status != VehicleStatus.ABATIDA) {
            throw new BusinessException("VEHICLE_DECOMMISSIONED", "Decommissioned vehicles cannot be reallocated");
        }

        vehicle.setStatus(status);
        return vehicle;
    }

    @Auditable(entityType = VEHICLE_ENTITY_TYPE, operation = AuditOperation.ELIMINACAO)
    @Transactional
    public void deleteVehicle(UUID vehicleId) {
        getVehicle(vehicleId).softDelete("system");
    }

    public Vehicle getVehicle(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found"));
    }

    public Page<Vehicle> listVehicles(VehicleStatus status, String location, Pageable pageable) {
        return vehicleRepository.findAllByFilters(status, blankToNull(location), pageable);
    }

    public Page<Vehicle> searchByPlate(String query, Pageable pageable) {
        return vehicleRepository.findByPlateContainingIgnoreCase(query, pageable);
    }

    public VehicleConsolidatedDto getConsolidated(UUID vehicleId) {
        Vehicle vehicle = getVehicle(vehicleId);
        return new VehicleConsolidatedDto(
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVehicleType(),
                vehicle.getStatus(),
                vehicle.getDocuments().stream().map(this::toDocumentDto).toList(),
                vehicle.getMaintenanceRecords().stream().map(this::toMaintenanceDto).toList(),
                vehicle.getChecklistInspections().stream().map(this::toChecklistDto).toList()
        );
    }

    private VehicleDocumentDto toDocumentDto(VehicleDocument document) {
        return new VehicleDocumentDto(
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getIssuingEntity(),
                document.getStatus(),
                document.getNotes(),
                document.getFile() == null ? null : document.getFile().getSizeBytes()
        );
    }

    private MaintenanceRecordDto toMaintenanceDto(MaintenanceRecord record) {
        return new MaintenanceRecordDto(
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

    private ChecklistInspectionDto toChecklistDto(ChecklistInspection inspection) {
        List<ChecklistInspectionDto.Item> items = inspection.getItems().stream()
                .map(item -> new ChecklistInspectionDto.Item(
                        item.getTemplateItem() == null ? null : item.getTemplateItem().getId(),
                        item.getItemName(),
                        item.isCritical(),
                        item.getStatus(),
                        item.getNotes()
                ))
                .toList();

        return new ChecklistInspectionDto(
                inspection.getActivityId(),
                inspection.getTemplate().getId(),
                inspection.getPerformedBy(),
                inspection.getPerformedAt(),
                inspection.getNotes(),
                items
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
