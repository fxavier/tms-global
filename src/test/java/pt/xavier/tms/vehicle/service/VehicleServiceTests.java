package pt.xavier.tms.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.vehicle.dto.VehicleCreateDto;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.mapper.ChecklistMapper;
import pt.xavier.tms.vehicle.mapper.MaintenanceMapper;
import pt.xavier.tms.vehicle.mapper.VehicleDocumentMapper;
import pt.xavier.tms.vehicle.mapper.VehicleMapper;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTests {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private VehicleDocumentMapper vehicleDocumentMapper;

    @Mock
    private MaintenanceMapper maintenanceMapper;

    @Mock
    private ChecklistMapper checklistMapper;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void createVehicleRejectsDuplicatePlate() {
        VehicleCreateDto dto = createDto("AA-11-BB");
        when(vehicleRepository.existsByPlate("AA-11-BB")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vehicle plate already exists");
    }

    @Test
    void deleteVehicleSoftDeletesExistingVehicle() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle(vehicleId);

        assertThat(vehicle.getDeletedAt()).isNotNull();
        assertThat(vehicle.getDeletedBy()).isEqualTo("system");
    }

    @Test
    void updateStatusRejectsReallocatingDecommissionedVehicle() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setStatus(VehicleStatus.ABATIDA);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateStatus(vehicleId, VehicleStatus.DISPONIVEL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Decommissioned vehicles cannot be reallocated");
    }

    @Test
    void createVehicleStoresDefaultStatusWhenNotProvided() {
        VehicleCreateDto dto = createDto("CC-22-DD");
        Vehicle mappedVehicle = new Vehicle();
        mappedVehicle.setPlate(dto.plate());
        when(vehicleRepository.existsByPlate("CC-22-DD")).thenReturn(false);
        when(vehicleMapper.toEntity(dto)).thenReturn(mappedVehicle);
        when(vehicleRepository.save(org.mockito.ArgumentMatchers.any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle saved = vehicleService.createVehicle(dto);

        assertThat(saved.getStatus()).isEqualTo(VehicleStatus.DISPONIVEL);
        verify(vehicleRepository).save(saved);
    }

    private static VehicleCreateDto createDto(String plate) {
        return new VehicleCreateDto(
                plate,
                "Mercedes",
                "Actros",
                "TRUCK",
                12000,
                "Lisboa",
                LocalDate.now(),
                null,
                null,
                null
        );
    }
}
