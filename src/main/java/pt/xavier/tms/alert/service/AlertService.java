package pt.xavier.tms.alert.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.alert.entity.Alert;
import pt.xavier.tms.alert.entity.AlertConfiguration;
import pt.xavier.tms.alert.repository.AlertConfigurationRepository;
import pt.xavier.tms.alert.repository.AlertRepository;
import pt.xavier.tms.driver.repository.DriverDocumentRepository;
import pt.xavier.tms.shared.enums.AlertSeverity;
import pt.xavier.tms.shared.enums.AlertType;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.vehicle.repository.MaintenanceRepository;
import pt.xavier.tms.vehicle.repository.VehicleDocumentRepository;

@Service
@ConditionalOnProperty(name = "tms.alert.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class AlertService {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final String VEHICLE_DOCUMENT_ENTITY = "VEHICLE_DOCUMENT";
    private static final String DRIVER_DOCUMENT_ENTITY = "DRIVER_DOCUMENT";
    private static final String MAINTENANCE_RECORD_ENTITY = "MAINTENANCE_RECORD";

    private final AlertRepository alertRepository;
    private final AlertConfigurationRepository alertConfigRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final DriverDocumentRepository driverDocumentRepository;
    private final MaintenanceRepository maintenanceRepository;

    public AlertService(
            AlertRepository alertRepository,
            AlertConfigurationRepository alertConfigRepository,
            VehicleDocumentRepository vehicleDocumentRepository,
            DriverDocumentRepository driverDocumentRepository,
            MaintenanceRepository maintenanceRepository
    ) {
        this.alertRepository = alertRepository;
        this.alertConfigRepository = alertConfigRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.driverDocumentRepository = driverDocumentRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional
    public void checkDocumentExpiry() {
        LocalDate today = LocalDate.now();

        AlertConfiguration vehicleConfig = getConfig(AlertType.DOCUMENT_EXPIRING, VEHICLE_DOCUMENT_ENTITY);
        AlertConfiguration driverConfig = getConfig(AlertType.DOCUMENT_EXPIRING, DRIVER_DOCUMENT_ENTITY);

        LocalDate vehicleWarningLimit = today.plusDays(vehicleConfig.getDaysBeforeWarning());
        LocalDate vehicleCriticalLimit = today.plusDays(vehicleConfig.getDaysBeforeCritical());

        vehicleDocumentRepository.findByExpiryDateBetweenAndStatusNot(today, vehicleWarningLimit, DocumentStatus.CANCELADO)
                .forEach(document -> {
                    AlertSeverity severity = !document.getExpiryDate().isAfter(vehicleCriticalLimit)
                            ? AlertSeverity.CRITICO
                            : AlertSeverity.AVISO;
                    createAlertIfNotExists(
                            AlertType.DOCUMENT_EXPIRING,
                            VEHICLE_DOCUMENT_ENTITY,
                            document.getId(),
                            severity,
                            "Vehicle document expiring",
                            "Vehicle document " + document.getDocumentType() + " is expiring soon"
                    );
                });

        vehicleDocumentRepository.findByExpiryDateBeforeAndStatusNot(today, DocumentStatus.CANCELADO)
                .forEach(document -> {
                    document.setStatus(DocumentStatus.EXPIRADO);
                    createAlertIfNotExists(
                            AlertType.DOCUMENT_EXPIRED,
                            VEHICLE_DOCUMENT_ENTITY,
                            document.getId(),
                            AlertSeverity.CRITICO,
                            "Vehicle document expired",
                            "Vehicle document " + document.getDocumentType() + " is expired"
                    );
                });

        LocalDate driverWarningLimit = today.plusDays(driverConfig.getDaysBeforeWarning());
        LocalDate driverCriticalLimit = today.plusDays(driverConfig.getDaysBeforeCritical());

        driverDocumentRepository.findByExpiryDateBetweenAndStatusNot(today, driverWarningLimit, DocumentStatus.CANCELADO)
                .forEach(document -> {
                    AlertSeverity severity = !document.getExpiryDate().isAfter(driverCriticalLimit)
                            ? AlertSeverity.CRITICO
                            : AlertSeverity.AVISO;
                    createAlertIfNotExists(
                            AlertType.DOCUMENT_EXPIRING,
                            DRIVER_DOCUMENT_ENTITY,
                            document.getId(),
                            severity,
                            "Driver document expiring",
                            "Driver document " + document.getDocumentType() + " is expiring soon"
                    );
                });

        driverDocumentRepository.findByExpiryDateBeforeAndStatusNot(today, DocumentStatus.CANCELADO)
                .forEach(document -> {
                    document.setStatus(DocumentStatus.EXPIRADO);
                    createAlertIfNotExists(
                            AlertType.DOCUMENT_EXPIRED,
                            DRIVER_DOCUMENT_ENTITY,
                            document.getId(),
                            AlertSeverity.CRITICO,
                            "Driver document expired",
                            "Driver document " + document.getDocumentType() + " is expired"
                    );
                });
    }

    @Transactional
    public void checkMaintenanceDue() {
        LocalDate today = LocalDate.now();
        AlertConfiguration config = getConfig(AlertType.MAINTENANCE_DUE, MAINTENANCE_RECORD_ENTITY);

        LocalDate warningLimit = today.plusDays(config.getDaysBeforeWarning());

        maintenanceRepository.findByNextMaintenanceDateBetween(today, warningLimit)
                .forEach(record -> createAlertIfNotExists(
                        AlertType.MAINTENANCE_DUE,
                        MAINTENANCE_RECORD_ENTITY,
                        record.getId(),
                        AlertSeverity.AVISO,
                        "Maintenance due",
                        "Maintenance is due soon for vehicle " + record.getVehicle().getPlate()
                ));

        maintenanceRepository.findByNextMaintenanceDateBefore(today)
                .forEach(record -> createAlertIfNotExists(
                        AlertType.MAINTENANCE_OVERDUE,
                        MAINTENANCE_RECORD_ENTITY,
                        record.getId(),
                        AlertSeverity.CRITICO,
                        "Maintenance overdue",
                        "Maintenance is overdue for vehicle " + record.getVehicle().getPlate()
                ));
    }

    @Transactional
    public void resolveObsoleteAlerts() {
        LocalDate today = LocalDate.now();
        alertRepository.findByAlertTypeInAndResolvedFalse(List.of(AlertType.DOCUMENT_EXPIRING, AlertType.DOCUMENT_EXPIRED))
                .forEach(alert -> {
                    if (shouldResolveDocumentAlert(alert, today)) {
                        alert.resolve(SYSTEM_USER);
                        alertRepository.save(alert);
                    }
                });
    }

    private boolean shouldResolveDocumentAlert(Alert alert, LocalDate today) {
        if (VEHICLE_DOCUMENT_ENTITY.equals(alert.getEntityType())) {
            return vehicleDocumentRepository.findById(alert.getEntityId())
                    .map(document -> {
                        if (document.getStatus() == DocumentStatus.CANCELADO) {
                            return true;
                        }
                        return switch (alert.getAlertType()) {
                            case DOCUMENT_EXPIRED -> document.getStatus() != DocumentStatus.EXPIRADO;
                            case DOCUMENT_EXPIRING -> document.getExpiryDate() == null || document.getExpiryDate().isBefore(today) || document.getStatus() == DocumentStatus.EXPIRADO;
                            default -> false;
                        };
                    })
                    .orElse(true);
        }

        if (DRIVER_DOCUMENT_ENTITY.equals(alert.getEntityType())) {
            return driverDocumentRepository.findById(alert.getEntityId())
                    .map(document -> {
                        if (document.getStatus() == DocumentStatus.CANCELADO) {
                            return true;
                        }
                        return switch (alert.getAlertType()) {
                            case DOCUMENT_EXPIRED -> document.getStatus() != DocumentStatus.EXPIRADO;
                            case DOCUMENT_EXPIRING -> document.getExpiryDate() == null || document.getExpiryDate().isBefore(today) || document.getStatus() == DocumentStatus.EXPIRADO;
                            default -> false;
                        };
                    })
                    .orElse(true);
        }

        return false;
    }

    private AlertConfiguration getConfig(AlertType type, String entityType) {
        return alertConfigRepository.findByAlertTypeAndEntityType(type, entityType)
                .filter(AlertConfiguration::isActive)
                .orElseGet(() -> AlertConfiguration.defaults(type, entityType));
    }

    @Transactional
    void createAlertIfNotExists(
            AlertType type,
            String entityType,
            UUID entityId,
            AlertSeverity severity,
            String title,
            String message
    ) {
        boolean exists = alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse(type, entityId);
        if (!exists) {
            Alert alert = new Alert();
            alert.setAlertType(type);
            alert.setEntityType(entityType);
            alert.setEntityId(entityId);
            alert.setSeverity(severity);
            alert.setTitle(title);
            alert.setMessage(message);
            alert.setResolved(false);
            alertRepository.save(alert);
            return;
        }

        Optional<Alert> existing = alertRepository.findByAlertTypeAndEntityIdAndResolvedFalse(type, entityId);
        existing.ifPresent(alert -> {
            if (severity.ordinal() > alert.getSeverity().ordinal()) {
                alert.setSeverity(severity);
                alertRepository.save(alert);
            }
        });
    }
}
