package pt.xavier.tms.activity.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.activity.dto.AllocationBlockerDto;
import pt.xavier.tms.activity.dto.AllocationResultDto;
import pt.xavier.tms.activity.repository.ActivityRepository;
import pt.xavier.tms.driver.repository.DriverDocumentRepository;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.integration.exception.RhIntegrationException;
import pt.xavier.tms.integration.port.RhIntegrationPort;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;
import pt.xavier.tms.shared.enums.DriverStatus;
import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.vehicle.repository.VehicleDocumentRepository;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@Service
@ConditionalOnProperty(name = "tms.activity.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class AllocationValidationService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final ChecklistInspectionRepository checklistInspectionRepository;
    private final DriverRepository driverRepository;
    private final DriverDocumentRepository driverDocumentRepository;
    private final RhIntegrationPort rhIntegrationPort;
    private final ActivityRepository activityRepository;

    public AllocationValidationService(
            VehicleRepository vehicleRepository,
            VehicleDocumentRepository vehicleDocumentRepository,
            ChecklistInspectionRepository checklistInspectionRepository,
            DriverRepository driverRepository,
            DriverDocumentRepository driverDocumentRepository,
            RhIntegrationPort rhIntegrationPort,
            ActivityRepository activityRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.checklistInspectionRepository = checklistInspectionRepository;
        this.driverRepository = driverRepository;
        this.driverDocumentRepository = driverDocumentRepository;
        this.rhIntegrationPort = rhIntegrationPort;
        this.activityRepository = activityRepository;
    }

    public AllocationResultDto validate(
            UUID activityId,
            UUID vehicleId,
            UUID driverId,
            OffsetDateTime plannedStart,
            OffsetDateTime plannedEnd,
            String rhOverrideJustification
    ) {
        List<AllocationBlockerDto> blockers = new ArrayList<>();

        vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
            if (vehicle.getStatus() == VehicleStatus.EM_MANUTENCAO) {
                blockers.add(blocker("VEHICLE_IN_MAINTENANCE", "Vehicle is under maintenance"));
            }
            if (vehicle.getStatus() == VehicleStatus.INDISPONIVEL) {
                blockers.add(blocker("VEHICLE_UNAVAILABLE", "Vehicle is unavailable"));
            }
            if (vehicle.getStatus() == VehicleStatus.ABATIDA) {
                blockers.add(blocker("VEHICLE_DECOMMISSIONED", "Vehicle is decommissioned"));
            }
        });

        vehicleDocumentRepository.findByVehicleIdAndStatus(vehicleId, DocumentStatus.EXPIRADO)
                .forEach(document -> blockers.add(blocker(
                        "VEHICLE_DOCUMENT_EXPIRED",
                        "Vehicle document expired: " + document.getDocumentType()
                )));

        checklistInspectionRepository.findLatestByVehicleId(vehicleId)
                .filter(inspection -> inspection.hasCriticalFailures())
                .ifPresent(inspection -> blockers.add(blocker(
                        "CHECKLIST_CRITICAL_FAILURE",
                        "Latest vehicle checklist has critical failures"
                )));

        driverRepository.findById(driverId).ifPresent(driver -> {
            if (driver.getStatus() == DriverStatus.INATIVO) {
                blockers.add(blocker("DRIVER_INACTIVE", "Driver is inactive"));
            }
            if (driver.getStatus() == DriverStatus.SUSPENSO) {
                blockers.add(blocker("DRIVER_SUSPENDED", "Driver is suspended"));
            }
        });

        driverDocumentRepository.findByDriverIdAndDocumentType(driverId, DriverDocumentType.CARTA_CONDUCAO).stream()
                .filter(document -> document.getStatus() == DocumentStatus.EXPIRADO)
                .forEach(document -> blockers.add(blocker(
                        "DRIVER_LICENSE_EXPIRED",
                        "Driver license is expired"
                )));

        LocalDate startDate = plannedStart.toLocalDate();
        LocalDate endDate = plannedEnd.toLocalDate();
        boolean hasOverride = rhOverrideJustification != null && !rhOverrideJustification.isBlank();

        try {
            var availability = rhIntegrationPort.checkAvailability(driverId, startDate, endDate);
            if (!availability.available() && !hasOverride) {
                blockers.add(blocker("DRIVER_RH_UNAVAILABLE", "Driver unavailable in RH system"));
            }
        } catch (RhIntegrationException ex) {
            if (!hasOverride) {
                blockers.add(blocker("RH_SYSTEM_UNAVAILABLE", "RH system is unavailable"));
            }
        }

        activityRepository.findConflictingActivitiesForVehicle(vehicleId, plannedStart, plannedEnd, activityId)
                .forEach(activity -> blockers.add(blocker(
                        "VEHICLE_ALLOCATION_CONFLICT",
                        "Vehicle already allocated to activity " + activity.getCode()
                )));

        activityRepository.findConflictingActivitiesForDriver(driverId, plannedStart, plannedEnd, activityId)
                .forEach(activity -> blockers.add(blocker(
                        "DRIVER_ALLOCATION_CONFLICT",
                        "Driver already allocated to activity " + activity.getCode()
                )));

        return new AllocationResultDto(blockers.isEmpty(), List.copyOf(blockers));
    }

    private static AllocationBlockerDto blocker(String code, String message) {
        return new AllocationBlockerDto(code, message);
    }
}
