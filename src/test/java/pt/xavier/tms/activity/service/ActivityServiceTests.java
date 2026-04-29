package pt.xavier.tms.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.xavier.tms.activity.dto.StatusTransitionDto;
import pt.xavier.tms.activity.entity.Activity;
import pt.xavier.tms.activity.entity.ActivityEvent;
import pt.xavier.tms.activity.repository.ActivityEventRepository;
import pt.xavier.tms.activity.repository.ActivityRepository;
import pt.xavier.tms.shared.enums.ActivityStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTests {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityEventRepository activityEventRepository;
    @Mock
    private ActivityCodeGenerator activityCodeGenerator;
    @Mock
    private AllocationValidationService allocationValidationService;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private ChecklistInspectionRepository checklistInspectionRepository;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void transitionPlaneadaToEmCursoSetsActualStart() {
        UUID activityId = UUID.randomUUID();
        Activity activity = activity(activityId, ActivityStatus.PLANEADA);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityEventRepository.save(any(ActivityEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = activityService.transitionStatus(activityId, new StatusTransitionDto(ActivityStatus.EM_CURSO, "start"));

        assertThat(response.status()).isEqualTo(ActivityStatus.EM_CURSO);
        assertThat(response.actualStart()).isNotNull();
    }

    @Test
    void transitionEmCursoToConcluidaSetsActualEnd() {
        UUID activityId = UUID.randomUUID();
        Activity activity = activity(activityId, ActivityStatus.EM_CURSO);
        activity.setActualStart(OffsetDateTime.now().minusHours(2));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityEventRepository.save(any(ActivityEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = activityService.transitionStatus(activityId, new StatusTransitionDto(ActivityStatus.CONCLUIDA, "done"));

        assertThat(response.status()).isEqualTo(ActivityStatus.CONCLUIDA);
        assertThat(response.actualEnd()).isNotNull();
    }

    @Test
    void transitionConcluidaToEmCursoThrowsBusinessException() {
        UUID activityId = UUID.randomUUID();
        Activity activity = activity(activityId, ActivityStatus.CONCLUIDA);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> activityService.transitionStatus(activityId, new StatusTransitionDto(ActivityStatus.EM_CURSO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid activity status transition");
    }

    @Test
    void transitionCanceladaToPlaneadaThrowsBusinessException() {
        UUID activityId = UUID.randomUUID();
        Activity activity = activity(activityId, ActivityStatus.CANCELADA);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> activityService.transitionStatus(activityId, new StatusTransitionDto(ActivityStatus.PLANEADA, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid activity status transition");
    }

    private static Activity activity(UUID id, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setCode("ACT-2026-0001");
        activity.setTitle("Delivery");
        activity.setActivityType("ENTREGA");
        activity.setLocation("Lisboa");
        activity.setPlannedStart(OffsetDateTime.now().plusHours(1));
        activity.setPlannedEnd(OffsetDateTime.now().plusHours(2));
        activity.setStatus(status);
        return activity;
    }
}
