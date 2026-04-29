package pt.xavier.tms.audit.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import pt.xavier.tms.shared.enums.AuditOperation;

public record AuditQueryDto(
        String entityType,
        UUID entityId,
        AuditOperation operation,
        String performedBy,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Integer page,
        Integer size
) {
}
