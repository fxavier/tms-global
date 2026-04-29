package pt.xavier.tms.audit.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.audit.dto.AuditQueryDto;
import pt.xavier.tms.audit.dto.AuditLogResponseDto;
import pt.xavier.tms.audit.entity.AuditLog;
import pt.xavier.tms.audit.service.AuditService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;

@RestController
@RequestMapping("/api/v1/audit")
@ConditionalOnProperty(name = "tms.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ApiResponse<PagedResponse<AuditLogResponseDto>> list(
            @ModelAttribute AuditQueryDto query
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, query.page() == null ? 0 : query.page()),
                clampSize(query.size() == null ? 20 : query.size()),
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        return ApiResponse.success(PagedResponse.from(
                auditService.list(
                                query.entityType(),
                                query.entityId(),
                                query.operation(),
                                query.performedBy(),
                                query.from(),
                                query.to(),
                                pageable
                        )
                        .map(AuditController::toResponse)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<AuditLogResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(auditService.getById(id))));
    }

    private static int clampSize(int size) {
        return Math.max(10, Math.min(size, 100));
    }

    private static AuditLogResponseDto toResponse(AuditLog log) {
        return new AuditLogResponseDto(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getOperation(),
                log.getPerformedBy(),
                log.getIpAddress(),
                log.getPreviousValues(),
                log.getNewValues(),
                log.getOccurredAt()
        );
    }
}
