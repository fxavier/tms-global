package pt.xavier.tms.alert.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.alert.entity.Alert;
import pt.xavier.tms.shared.enums.AlertSeverity;
import pt.xavier.tms.shared.enums.AlertType;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByResolvedFalse(Pageable pageable);

    Page<Alert> findByResolvedFalseAndSeverity(AlertSeverity severity, Pageable pageable);

    boolean existsByAlertTypeAndEntityIdAndResolvedFalse(AlertType type, UUID entityId);

    Optional<Alert> findByAlertTypeAndEntityIdAndResolvedFalse(AlertType type, UUID entityId);

    List<Alert> findByAlertTypeInAndResolvedFalse(List<AlertType> types);
}
