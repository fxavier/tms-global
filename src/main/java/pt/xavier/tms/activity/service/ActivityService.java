package pt.xavier.tms.activity.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.activity.dto.ActivityCreateDto;
import pt.xavier.tms.activity.dto.ActivityEventDto;
import pt.xavier.tms.activity.dto.ActivityResponseDto;
import pt.xavier.tms.activity.dto.ActivityUpdateDto;
import pt.xavier.tms.activity.dto.AllocationRequestDto;
import pt.xavier.tms.activity.dto.StatusTransitionDto;
import pt.xavier.tms.activity.entity.Activity;
import pt.xavier.tms.activity.entity.ActivityEvent;
import pt.xavier.tms.activity.repository.ActivityEventRepository;
import pt.xavier.tms.activity.repository.ActivityRepository;
import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.repository.DriverRepository;
import pt.xavier.tms.shared.enums.ActivityPriority;
import pt.xavier.tms.shared.enums.ActivityStatus;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.exception.AllocationException;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.vehicle.repository.VehicleRepository;

@Service
@Transactional(readOnly = true)
public class ActivityService {

    private static final String ACTIVITY_ENTITY_TYPE = "ACTIVITY";

    private final ActivityRepository activityRepository;
    private final ActivityEventRepository activityEventRepository;
    private final ActivityCodeGenerator activityCodeGenerator;
    private final AllocationValidationService allocationValidationService;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final ChecklistInspectionRepository checklistInspectionRepository;

    public ActivityService(
            ActivityRepository activityRepository,
            ActivityEventRepository activityEventRepository,
            ActivityCodeGenerator activityCodeGenerator,
            AllocationValidationService allocationValidationService,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            ChecklistInspectionRepository checklistInspectionRepository
    ) {
        this.activityRepository = activityRepository;
        this.activityEventRepository = activityEventRepository;
        this.activityCodeGenerator = activityCodeGenerator;
        this.allocationValidationService = allocationValidationService;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.checklistInspectionRepository = checklistInspectionRepository;
    }

    @Auditable(entityType = ACTIVITY_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public ActivityResponseDto createActivity(ActivityCreateDto dto) {
        Activity activity = new Activity();
        activity.setCode(activityCodeGenerator.generateActivityCode());
        activity.setStatus(ActivityStatus.PLANEADA);
        applyCreateDto(activity, dto);
        return toResponseDto(activityRepository.save(activity));
    }

    @Auditable(entityType = ACTIVITY_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public ActivityResponseDto updateActivity(UUID activityId, ActivityUpdateDto dto) {
        Activity activity = getActivityEntity(activityId);
        applyUpdateDto(activity, dto);
        return toResponseDto(activity);
    }

    @Auditable(entityType = ACTIVITY_ENTITY_TYPE, operation = AuditOperation.ELIMINACAO)
    @Transactional
    public void deleteActivity(UUID activityId) {
        getActivityEntity(activityId).softDelete(currentUser());
    }

    public ActivityResponseDto getActivity(UUID activityId) {
        return toResponseDto(getActivityEntity(activityId));
    }

    public Page<ActivityResponseDto> listActivities(
            ActivityStatus status,
            UUID vehicleId,
            UUID driverId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        return activityRepository.findAllByFilters(status, vehicleId, driverId, from, to, pageable)
                .map(this::toResponseDto);
    }

    @Auditable(entityType = ACTIVITY_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public ActivityResponseDto allocate(UUID activityId, AllocationRequestDto dto) {
        Activity activity = getActivityEntity(activityId);

        var result = allocationValidationService.validate(
                activityId,
                dto.vehicleId(),
                dto.driverId(),
                activity.getPlannedStart(),
                activity.getPlannedEnd(),
                dto.rhOverrideJustification()
        );

        if (!result.allocated()) {
            String details = result.blockers().stream()
                    .map(blocker -> blocker.code() + ": " + blocker.message())
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("Allocation blocked");
            throw new AllocationException("ALLOCATION_BLOCKED", details);
        }

        activity.setVehicle(vehicleRepository.findById(dto.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found")));
        activity.setDriver(driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver not found")));
        activity.setRhOverrideJustification(dto.rhOverrideJustification());

        saveEvent(activity, "ALLOCATION", activity.getStatus(), activity.getStatus(), "Allocation completed");
        return toResponseDto(activity);
    }

    @Auditable(entityType = ACTIVITY_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public ActivityResponseDto transitionStatus(UUID activityId, StatusTransitionDto dto) {
        Activity activity = getActivityEntity(activityId);
        ActivityStatus currentStatus = activity.getStatus();
        ActivityStatus newStatus = dto.newStatus();

        currentStatus.validateTransition(newStatus);

        if (currentStatus == ActivityStatus.PLANEADA && newStatus == ActivityStatus.EM_CURSO) {
            if (activity.getVehicle() != null) {
                checklistInspectionRepository.findLatestByVehicleId(activity.getVehicle().getId())
                        .filter(inspection -> inspection.hasCriticalFailures())
                        .ifPresent(inspection -> {
                            throw new BusinessException(
                                    "CHECKLIST_CRITICAL_FAILURE",
                                    "Cannot start activity because latest checklist has critical failures"
                            );
                        });
            }
            activity.setActualStart(OffsetDateTime.now());
        }

        if (newStatus == ActivityStatus.CONCLUIDA || newStatus == ActivityStatus.CANCELADA) {
            activity.setActualEnd(OffsetDateTime.now());
        }

        activity.setStatus(newStatus);
        saveEvent(activity, "STATUS_TRANSITION", currentStatus, newStatus, dto.notes());
        return toResponseDto(activity);
    }

    public List<ActivityEventDto> getEvents(UUID activityId) {
        getActivityEntity(activityId);
        return activityEventRepository.findByActivityIdOrderByPerformedAtAsc(activityId).stream()
                .map(event -> new ActivityEventDto(
                        event.getId(),
                        event.getEventType(),
                        event.getPreviousStatus(),
                        event.getNewStatus(),
                        event.getPerformedBy(),
                        event.getPerformedAt(),
                        event.getNotes()
                ))
                .toList();
    }

    private Activity getActivityEntity(UUID activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("ACTIVITY_NOT_FOUND", "Activity not found"));
    }

    private void applyCreateDto(Activity activity, ActivityCreateDto dto) {
        activity.setTitle(dto.title());
        activity.setActivityType(dto.activityType());
        activity.setLocation(dto.location());
        activity.setPlannedStart(dto.plannedStart());
        activity.setPlannedEnd(dto.plannedEnd());
        activity.setPriority(dto.priority() == null ? ActivityPriority.NORMAL : dto.priority());
        activity.setDescription(dto.description());
        activity.setNotes(dto.notes());
        activity.setRhOverrideJustification(dto.rhOverrideJustification());
        if (dto.vehicleId() != null) {
            activity.setVehicle(vehicleRepository.findById(dto.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found")));
        }
        if (dto.driverId() != null) {
            activity.setDriver(driverRepository.findById(dto.driverId())
                    .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver not found")));
        }
    }

    private void applyUpdateDto(Activity activity, ActivityUpdateDto dto) {
        if (dto.title() != null) {
            activity.setTitle(dto.title());
        }
        if (dto.activityType() != null) {
            activity.setActivityType(dto.activityType());
        }
        if (dto.location() != null) {
            activity.setLocation(dto.location());
        }
        if (dto.plannedStart() != null) {
            activity.setPlannedStart(dto.plannedStart());
        }
        if (dto.plannedEnd() != null) {
            activity.setPlannedEnd(dto.plannedEnd());
        }
        if (dto.priority() != null) {
            activity.setPriority(dto.priority());
        }
        if (dto.description() != null) {
            activity.setDescription(dto.description());
        }
        if (dto.notes() != null) {
            activity.setNotes(dto.notes());
        }
        if (dto.rhOverrideJustification() != null) {
            activity.setRhOverrideJustification(dto.rhOverrideJustification());
        }
        if (dto.vehicleId() != null) {
            activity.setVehicle(vehicleRepository.findById(dto.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found")));
        }
        if (dto.driverId() != null) {
            Driver driver = driverRepository.findById(dto.driverId())
                    .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver not found"));
            activity.setDriver(driver);
        }
    }

    private void saveEvent(
            Activity activity,
            String eventType,
            ActivityStatus previousStatus,
            ActivityStatus newStatus,
            String notes
    ) {
        ActivityEvent event = new ActivityEvent();
        event.setActivity(activity);
        event.setEventType(eventType);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setPerformedBy(currentUser());
        event.setPerformedAt(OffsetDateTime.now());
        event.setNotes(notes);
        activityEventRepository.save(event);
    }

    private ActivityResponseDto toResponseDto(Activity activity) {
        return new ActivityResponseDto(
                activity.getId(),
                activity.getCode(),
                activity.getTitle(),
                activity.getActivityType(),
                activity.getLocation(),
                activity.getPlannedStart(),
                activity.getPlannedEnd(),
                activity.getActualStart(),
                activity.getActualEnd(),
                activity.getPriority(),
                activity.getStatus(),
                activity.getVehicle() == null ? null : activity.getVehicle().getId(),
                activity.getDriver() == null ? null : activity.getDriver().getId(),
                activity.getDescription(),
                activity.getNotes(),
                activity.getRhOverrideJustification()
        );
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "system";
        }
        return authentication.getName();
    }
}
