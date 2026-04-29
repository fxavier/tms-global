package pt.xavier.tms.vehicle.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.vehicle.dto.MaintenanceRecordDto;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.event.MaintenanceDueAlertRequested;
import pt.xavier.tms.vehicle.repository.MaintenanceRepository;

@Service
@ConditionalOnProperty(name = "tms.vehicle.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class MaintenanceService {

    private static final String MAINTENANCE_ENTITY_TYPE = "MAINTENANCE_RECORD";

    private final VehicleService vehicleService;
    private final MaintenanceRepository maintenanceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MaintenanceService(
            VehicleService vehicleService,
            MaintenanceRepository maintenanceRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.vehicleService = vehicleService;
        this.maintenanceRepository = maintenanceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Auditable(entityType = MAINTENANCE_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public MaintenanceRecord registerMaintenance(UUID vehicleId, MaintenanceRecordDto dto) {
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        applyDto(record, dto);

        MaintenanceRecord saved = maintenanceRepository.save(record);
        if (saved.getNextMaintenanceDate() != null) {
            eventPublisher.publishEvent(new MaintenanceDueAlertRequested(
                    vehicleId,
                    saved.getId(),
                    saved.getNextMaintenanceDate()
            ));
        }
        return saved;
    }

    public Page<MaintenanceRecord> listMaintenance(UUID vehicleId, Pageable pageable) {
        vehicleService.getVehicle(vehicleId);
        return maintenanceRepository.findByVehicleId(vehicleId, pageable);
    }

    public MaintenanceRecord getMaintenance(UUID vehicleId, UUID maintenanceId) {
        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("MAINTENANCE_NOT_FOUND", "Maintenance record not found"));
        if (!record.getVehicle().getId().equals(vehicleId)) {
            throw new ResourceNotFoundException("MAINTENANCE_NOT_FOUND", "Maintenance record not found");
        }
        return record;
    }

    private static void applyDto(MaintenanceRecord record, MaintenanceRecordDto dto) {
        record.setMaintenanceType(dto.maintenanceType());
        record.setPerformedAt(dto.performedAt());
        record.setMileageAtService(dto.mileageAtService());
        record.setDescription(dto.description());
        record.setSupplier(dto.supplier());
        record.setTotalCost(dto.totalCost());
        record.setPartsReplaced(dto.partsReplaced());
        record.setNextMaintenanceDate(dto.nextMaintenanceDate());
        record.setNextMaintenanceMileage(dto.nextMaintenanceMileage());
        record.setResponsibleUser(dto.responsibleUser());
    }
}
