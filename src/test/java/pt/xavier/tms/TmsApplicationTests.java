package pt.xavier.tms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration",
        "tms.jpa.auditing.enabled=false",
        "tms.audit.enabled=false",
        "tms.vehicle.controllers.enabled=false",
        "tms.vehicle.services.enabled=false",
        "tms.driver.controllers.enabled=false",
        "tms.driver.services.enabled=false"
})
class TmsApplicationTests {

    @Test
    void contextLoads() {
    }
}
