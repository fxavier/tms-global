package pt.xavier.tms.alert.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.alert.entity.AlertConfiguration;
import pt.xavier.tms.shared.enums.AlertType;

public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, UUID> {

    Optional<AlertConfiguration> findByAlertTypeAndEntityType(AlertType type, String entityType);
}
