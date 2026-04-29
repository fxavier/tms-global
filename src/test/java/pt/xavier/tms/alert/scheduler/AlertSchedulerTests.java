package pt.xavier.tms.alert.scheduler;

import static org.mockito.Mockito.inOrder;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import pt.xavier.tms.alert.service.AlertService;

@ExtendWith(MockitoExtension.class)
class AlertSchedulerTests {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertScheduler alertScheduler;

    @Test
    void runDailyAlertCheckCallsAlertChecksInOrder() {
        alertScheduler.runDailyAlertCheck();

        InOrder order = inOrder(alertService);
        order.verify(alertService).checkDocumentExpiry();
        order.verify(alertService).checkMaintenanceDue();
        order.verify(alertService).resolveObsoleteAlerts();
    }

    @Test
    void runDailyAlertCheckUsesDefaultSixAmCron() throws NoSuchMethodException {
        Method method = AlertScheduler.class.getDeclaredMethod("runDailyAlertCheck");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        org.assertj.core.api.Assertions.assertThat(scheduled).isNotNull();
        org.assertj.core.api.Assertions.assertThat(scheduled.cron())
                .isEqualTo("${tms.scheduling.alert-check-cron:0 0 6 * * *}");
    }
}
