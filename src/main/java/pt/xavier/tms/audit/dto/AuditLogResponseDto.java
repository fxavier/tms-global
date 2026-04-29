package pt.xavier.tms.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import pt.xavier.tms.shared.enums.AuditOperation;

public record AuditLogResponseDto(
        UUID id,
        String entityType,
        UUID entityId,
        AuditOperation operation,
        String performedBy,
        String ipAddress,
        Map<String, Object> previousValues,
        Map<String, Object> newValues,
        Instant occurredAt
) {
}
