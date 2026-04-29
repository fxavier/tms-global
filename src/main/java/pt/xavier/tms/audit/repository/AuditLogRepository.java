package pt.xavier.tms.audit.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pt.xavier.tms.audit.entity.AuditLog;
import pt.xavier.tms.shared.enums.AuditOperation;

public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog log);

    Optional<AuditLog> findById(UUID id);

    @Query("""
            select a
            from AuditLog a
            where (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:operation is null or a.operation = :operation)
              and (:performedBy is null or a.performedBy = :performedBy)
              and (:from is null or a.occurredAt >= :from)
              and (:to is null or a.occurredAt <= :to)
            """)
    Page<AuditLog> findByFilters(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("operation") AuditOperation operation,
            @Param("performedBy") String performedBy,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
