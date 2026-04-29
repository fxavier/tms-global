package pt.xavier.tms.audit.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import pt.xavier.tms.shared.enums.AuditOperation;

public record AuditEvent(
        String entityType,
        UUID entityId,
        AuditOperation operation,
        String performedBy,
        String ipAddress,
        Map<String, Object> previousValues,
        Map<String, Object> newValues,
        Instant occurredAt
) {

    public static AuditEvent of(
            String entityType,
            UUID entityId,
            AuditOperation operation,
            String performedBy,
            String ipAddress,
            Map<String, Object> previousValues,
            Map<String, Object> newValues
    ) {
        return new AuditEvent(
                entityType,
                entityId,
                operation,
                performedBy,
                ipAddress,
                previousValues,
                newValues,
                Instant.now()
        );
    }
}
