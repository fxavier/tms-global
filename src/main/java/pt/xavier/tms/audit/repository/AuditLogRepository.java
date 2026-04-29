package pt.xavier.tms.audit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.Repository;

import pt.xavier.tms.audit.entity.AuditLog;

public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog log);

    Optional<AuditLog> findById(UUID id);

    Page<AuditLog> findAll(Specification<AuditLog> specification, Pageable pageable);
}
