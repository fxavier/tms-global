package pt.xavier.tms.alert.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.alert.entity.Alert;
import pt.xavier.tms.alert.repository.AlertRepository;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Service
@ConditionalOnProperty(name = "tms.alert.services.enabled", havingValue = "true", matchIfMissing = true)
public class AlertResolutionService {

    private final AlertRepository alertRepository;

    public AlertResolutionService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert resolveManually(UUID alertId, String resolvedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("ALERT_NOT_FOUND", "Alert not found"));

        if (!alert.isResolved()) {
            alert.resolve(resolvedBy);
            alertRepository.save(alert);
        }

        return alert;
    }
}
