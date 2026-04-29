package pt.xavier.tms.alert.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.Predicate;
import pt.xavier.tms.alert.dto.AlertResponseDto;
import pt.xavier.tms.alert.entity.Alert;
import pt.xavier.tms.alert.repository.AlertRepository;
import pt.xavier.tms.alert.service.AlertResolutionService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.shared.enums.AlertSeverity;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/alerts")
@ConditionalOnProperty(name = "tms.alert.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertResolutionService alertResolutionService;

    public AlertController(AlertRepository alertRepository, AlertResolutionService alertResolutionService) {
        this.alertRepository = alertRepository;
        this.alertResolutionService = alertResolutionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<AlertResponseDto>> list(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Alert> specification = withFilters(resolved, severity, entityType);

        return ApiResponse.success(PagedResponse.from(
                alertRepository.findAll(specification, pageable).map(AlertController::toResponse)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','AUDITOR')")
    public ApiResponse<AlertResponseDto> getById(@PathVariable UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ALERT_NOT_FOUND", "Alert not found"));
        return ApiResponse.success(toResponse(alert));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<AlertResponseDto> resolve(@PathVariable UUID id) {
        return ApiResponse.success(toResponse(alertResolutionService.resolveManually(id, currentUsername())));
    }

    private static Specification<Alert> withFilters(Boolean resolved, AlertSeverity severity, String entityType) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (resolved != null) {
                predicate = cb.and(predicate, cb.equal(root.get("resolved"), resolved));
            }
            if (severity != null) {
                predicate = cb.and(predicate, cb.equal(root.get("severity"), severity));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("entityType"), entityType.trim()));
            }

            return predicate;
        };
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private static int clampSize(int size) {
        return Math.max(10, Math.min(size, 100));
    }

    private static AlertResponseDto toResponse(Alert alert) {
        return new AlertResponseDto(
                alert.getId(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getEntityType(),
                alert.getEntityId(),
                alert.getTitle(),
                alert.getMessage(),
                alert.isResolved(),
                alert.getResolvedAt(),
                alert.getResolvedBy(),
                alert.getCreatedAt()
        );
    }
}
