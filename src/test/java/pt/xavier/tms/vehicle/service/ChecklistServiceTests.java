package pt.xavier.tms.vehicle.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.shared.enums.ChecklistItemStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.vehicle.dto.ChecklistInspectionDto;
import pt.xavier.tms.vehicle.entity.ChecklistTemplate;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.vehicle.repository.ChecklistTemplateRepository;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTests {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private ChecklistInspectionRepository checklistInspectionRepository;

    @InjectMocks
    private ChecklistService checklistService;

    @Test
    void submitChecklistRejectsCriticalFailures() {
        UUID vehicleId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        ChecklistTemplate template = new ChecklistTemplate();
        template.setId(templateId);

        when(vehicleService.getVehicle(vehicleId)).thenReturn(vehicle);
        when(checklistTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        ChecklistInspectionDto dto = new ChecklistInspectionDto(
                null,
                templateId,
                "driver-1",
                null,
                null,
                List.of(new ChecklistInspectionDto.Item(
                        null,
                        "Brakes",
                        true,
                        ChecklistItemStatus.AVARIA,
                        null
                ))
        );

        assertThatThrownBy(() -> checklistService.submitChecklist(vehicleId, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Checklist contains critical failures");
    }

    @Test
    void submitChecklistAcceptsNonCriticalInspection() {
        UUID vehicleId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        ChecklistTemplate template = new ChecklistTemplate();
        template.setId(templateId);

        when(vehicleService.getVehicle(vehicleId)).thenReturn(vehicle);
        when(checklistTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(checklistInspectionRepository.save(org.mockito.ArgumentMatchers.any(ChecklistInspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistInspectionDto dto = new ChecklistInspectionDto(
                null,
                templateId,
                "driver-1",
                null,
                null,
                List.of(new ChecklistInspectionDto.Item(
                        null,
                        "Brakes",
                        true,
                        ChecklistItemStatus.OK,
                        null
                ))
        );

        ChecklistInspection inspection = checklistService.submitChecklist(vehicleId, dto);

        assertThat(inspection.getVehicle()).isEqualTo(vehicle);
        assertThat(inspection.hasCriticalFailures()).isFalse();
        verify(checklistInspectionRepository).save(inspection);
    }
}
