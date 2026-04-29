package pt.xavier.tms.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.activity.entity.Activity;
import pt.xavier.tms.activity.repository.ActivityRepository;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.entity.DriverDocument;
import pt.xavier.tms.driver.repository.DriverDocumentRepository;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.integration.exception.RhIntegrationException;
import pt.xavier.tms.integration.port.RhIntegrationPort;
import pt.xavier.tms.shared.enums.ChecklistItemStatus;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;
import pt.xavier.tms.shared.enums.DriverStatus;
import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.entity.ChecklistInspectionItem;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.VehicleDocument;
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.vehicle.repository.VehicleDocumentRepository;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class AllocationValidationServiceTests {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;
    @Mock
    private ChecklistInspectionRepository checklistInspectionRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private DriverDocumentRepository driverDocumentRepository;
    @Mock
    private RhIntegrationPort rhIntegrationPort;
    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private AllocationValidationService service;

    private UUID activityId;
    private UUID vehicleId;
    private UUID driverId;
    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        activityId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        start = OffsetDateTime.now().plusDays(1);
        end = start.plusHours(2);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle(VehicleStatus.DISPONIVEL)));
        when(vehicleDocumentRepository.findByVehicleIdAndStatus(vehicleId, DocumentStatus.EXPIRADO)).thenReturn(List.of());
        when(checklistInspectionRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.empty());
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver(DriverStatus.ATIVO)));
        when(driverDocumentRepository.findByDriverIdAndDocumentType(driverId, DriverDocumentType.CARTA_CONDUCAO)).thenReturn(List.of());
        when(rhIntegrationPort.checkAvailability(driverId, start.toLocalDate(), end.toLocalDate()))
                .thenReturn(new DriverAvailabilityDto(driverId, true, null, List.of()));
        when(activityRepository.findConflictingActivitiesForVehicle(vehicleId, start, end, activityId)).thenReturn(List.of());
        when(activityRepository.findConflictingActivitiesForDriver(driverId, start, end, activityId)).thenReturn(List.of());
    }

    @Test
    void vehicleInMaintenanceGeneratesBlocker() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle(VehicleStatus.EM_MANUTENCAO)));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("VEHICLE_IN_MAINTENANCE");
    }

    @Test
    void decommissionedVehicleGeneratesBlocker() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle(VehicleStatus.ABATIDA)));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("VEHICLE_DECOMMISSIONED");
    }

    @Test
    void expiredVehicleDocumentGeneratesBlocker() {
        VehicleDocument document = new VehicleDocument();
        document.setStatus(DocumentStatus.EXPIRADO);
        when(vehicleDocumentRepository.findByVehicleIdAndStatus(vehicleId, DocumentStatus.EXPIRADO)).thenReturn(List.of(document));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("VEHICLE_DOCUMENT_EXPIRED");
    }

    @Test
    void criticalChecklistFailureGeneratesBlocker() {
        ChecklistInspection inspection = new ChecklistInspection();
        ChecklistInspectionItem item = new ChecklistInspectionItem();
        item.setCritical(true);
        item.setStatus(ChecklistItemStatus.AVARIA);
        inspection.setItems(List.of(item));
        when(checklistInspectionRepository.findLatestByVehicleId(vehicleId)).thenReturn(Optional.of(inspection));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("CHECKLIST_CRITICAL_FAILURE");
    }

    @Test
    void suspendedDriverGeneratesBlocker() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver(DriverStatus.SUSPENSO)));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("DRIVER_SUSPENDED");
    }

    @Test
    void expiredLicenseGeneratesBlocker() {
        DriverDocument document = new DriverDocument();
        document.setStatus(DocumentStatus.EXPIRADO);
        when(driverDocumentRepository.findByDriverIdAndDocumentType(driverId, DriverDocumentType.CARTA_CONDUCAO))
                .thenReturn(List.of(document));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("DRIVER_LICENSE_EXPIRED");
    }

    @Test
    void rhUnavailableWithoutOverrideGeneratesBlocker() {
        when(rhIntegrationPort.checkAvailability(driverId, start.toLocalDate(), end.toLocalDate()))
                .thenReturn(new DriverAvailabilityDto(driverId, false, "ON_LEAVE", List.of()));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("DRIVER_RH_UNAVAILABLE");
    }

    @Test
    void rhExceptionWithoutOverrideGeneratesBlocker() {
        when(rhIntegrationPort.checkAvailability(driverId, start.toLocalDate(), end.toLocalDate()))
                .thenThrow(new RhIntegrationException("down", new RuntimeException("boom")));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("RH_SYSTEM_UNAVAILABLE");
    }

    @Test
    void rhExceptionWithOverrideDoesNotBlock() {
        when(rhIntegrationPort.checkAvailability(driverId, start.toLocalDate(), end.toLocalDate()))
                .thenThrow(new RhIntegrationException("down", new RuntimeException("boom")));

        var result = service.validate(activityId, vehicleId, driverId, start, end, "Justified");

        assertThat(result.blockers()).extracting("code").doesNotContain("RH_SYSTEM_UNAVAILABLE");
    }

    @Test
    void vehicleConflictGeneratesBlocker() {
        Activity conflict = new Activity();
        conflict.setCode("ACT-2026-0001");
        when(activityRepository.findConflictingActivitiesForVehicle(vehicleId, start, end, activityId)).thenReturn(List.of(conflict));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("VEHICLE_ALLOCATION_CONFLICT");
    }

    @Test
    void driverConflictGeneratesBlocker() {
        Activity conflict = new Activity();
        conflict.setCode("ACT-2026-0002");
        when(activityRepository.findConflictingActivitiesForDriver(driverId, start, end, activityId)).thenReturn(List.of(conflict));

        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isFalse();
        assertThat(result.blockers()).extracting("code").contains("DRIVER_ALLOCATION_CONFLICT");
    }

    @Test
    void validAllocationReturnsAllocatedTrueWithNoBlockers() {
        var result = service.validate(activityId, vehicleId, driverId, start, end, null);

        assertThat(result.allocated()).isTrue();
        assertThat(result.blockers()).isEmpty();
    }

    private static Vehicle vehicle(VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setStatus(status);
        return vehicle;
    }

    private static Driver driver(DriverStatus status) {
        Driver driver = new Driver();
        driver.setId(UUID.randomUUID());
        driver.setStatus(status);
        return driver;
    }
}
