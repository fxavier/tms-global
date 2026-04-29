package pt.xavier.tms.alert.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import pt.xavier.tms.shared.enums.AlertSeverity;
import pt.xavier.tms.shared.enums.AlertType;

public record AlertResponseDto(
        UUID id,
        AlertType alertType,
        AlertSeverity severity,
        String entityType,
        UUID entityId,
        String title,
        String message,
        boolean resolved,
        OffsetDateTime resolvedAt,
        String resolvedBy,
        Instant createdAt
) {
}
