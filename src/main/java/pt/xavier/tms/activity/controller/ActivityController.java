package pt.xavier.tms.activity.controller;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.activity.dto.ActivityCreateDto;
import pt.xavier.tms.activity.dto.ActivityEventDto;
import pt.xavier.tms.activity.dto.ActivityResponseDto;
import pt.xavier.tms.activity.dto.ActivityUpdateDto;
import pt.xavier.tms.activity.dto.AllocationRequestDto;
import pt.xavier.tms.activity.dto.StatusTransitionDto;
import pt.xavier.tms.activity.service.ActivityService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.shared.enums.ActivityStatus;

@RestController
@RequestMapping("/api/v1/activities")
@ConditionalOnProperty(name = "tms.activity.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR')")
    public ResponseEntity<ApiResponse<ActivityResponseDto>> createActivity(@Valid @RequestBody ActivityCreateDto request) {
        ActivityResponseDto response = activityService.createActivity(request);
        return ResponseEntity.created(URI.create("/api/v1/activities/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<ActivityResponseDto>> listActivities(
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(
                activityService.listActivities(status, vehicleId, driverId, from, to, Pageables.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR') or (hasRole('MOTORISTA') and @activitySecurityService.isAssignedDriver(#id))")
    public ApiResponse<ActivityResponseDto> getActivity(@PathVariable UUID id) {
        return ApiResponse.success(activityService.getActivity(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR')")
    public ApiResponse<ActivityResponseDto> updateActivity(@PathVariable UUID id, @Valid @RequestBody ActivityUpdateDto request) {
        return ApiResponse.success(activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<Void> deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR')")
    public ApiResponse<ActivityResponseDto> allocate(@PathVariable UUID id, @Valid @RequestBody AllocationRequestDto request) {
        return ApiResponse.success(activityService.allocate(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','MOTORISTA')")
    public ApiResponse<ActivityResponseDto> transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusTransitionDto request
    ) {
        return ApiResponse.success(activityService.transitionStatus(id, request));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','AUDITOR')")
    public ApiResponse<List<ActivityEventDto>> getEvents(@PathVariable UUID id) {
        return ApiResponse.success(activityService.getEvents(id));
    }
}
