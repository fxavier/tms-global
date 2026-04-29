package pt.xavier.tms.vehicle.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import pt.xavier.tms.shared.enums.MaintenanceType;
import pt.xavier.tms.vehicle.dto.MaintenanceRecordDto;
import pt.xavier.tms.vehicle.entity.MaintenanceRecord;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.event.MaintenanceDueAlertRequested;
import pt.xavier.tms.vehicle.repository.MaintenanceRepository;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTests {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    void registerMaintenancePublishesAlertRequestWhenNextMaintenanceDateExists() {
        UUID vehicleId = UUID.randomUUID();
        LocalDate nextMaintenanceDate = LocalDate.now().plusMonths(6);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);

        when(vehicleService.getVehicle(vehicleId)).thenReturn(vehicle);
        when(maintenanceRepository.save(any(MaintenanceRecord.class))).thenAnswer(invocation -> {
            MaintenanceRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        maintenanceService.registerMaintenance(vehicleId, new MaintenanceRecordDto(
                MaintenanceType.PREVENTIVA,
                LocalDate.now(),
                null,
                "Preventive maintenance",
                null,
                null,
                null,
                nextMaintenanceDate,
                null,
                "system"
        ));

        verify(eventPublisher).publishEvent(any(MaintenanceDueAlertRequested.class));
    }
}
