package pt.xavier.tms.alert.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pt.xavier.tms.shared.enums.AlertType;

@Getter
@Setter
@Entity
@Table(name = "alert_configurations")
@NoArgsConstructor
public class AlertConfiguration extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "days_before_warning", nullable = false)
    private Integer daysBeforeWarning = 30;

    @Column(name = "days_before_critical", nullable = false)
    private Integer daysBeforeCritical = 7;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public static AlertConfiguration defaults(AlertType alertType, String entityType) {
        AlertConfiguration configuration = new AlertConfiguration();
        configuration.setAlertType(alertType);
        configuration.setEntityType(entityType);
        configuration.setDaysBeforeWarning(30);
        configuration.setDaysBeforeCritical(7);
        configuration.setActive(true);
        return configuration;
    }
}
