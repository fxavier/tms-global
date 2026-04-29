package pt.xavier.tms.audit.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import pt.xavier.tms.shared.enums.AuditOperation;

@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30)
    private AuditOperation operation;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_values", columnDefinition = "jsonb")
    private Map<String, Object> previousValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @Column(name = "created_at", nullable = false)
    private Instant occurredAt;

    public static AuditLog of(
            String entityType,
            UUID entityId,
            AuditOperation operation,
            String performedBy,
            String ipAddress,
            Map<String, Object> previousValues,
            Map<String, Object> newValues,
            Instant occurredAt
    ) {
        AuditLog log = new AuditLog();
        log.entityType = entityType;
        log.entityId = entityId;
        log.operation = operation;
        log.performedBy = performedBy;
        log.ipAddress = ipAddress;
        log.previousValues = previousValues;
        log.newValues = newValues;
        log.occurredAt = occurredAt;
        return log;
    }
}
