package pt.xavier.tms.alert.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.xavier.tms.alert.service.AlertService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tms.alert.services.enabled", havingValue = "true", matchIfMissing = true)
public class AlertScheduler {

    private final AlertService alertService;

    @Scheduled(cron = "${tms.scheduling.alert-check-cron:0 0 6 * * *}")
    @Transactional
    public void runDailyAlertCheck() {
        log.info("Starting daily alert checks");
        alertService.checkDocumentExpiry();
        alertService.checkMaintenanceDue();
        alertService.resolveObsoleteAlerts();
        log.info("Finished daily alert checks");
    }
}
