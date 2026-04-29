package pt.xavier.tms.vehicle.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pt.xavier.tms.shared.enums.MaintenanceType;

@Getter
@Setter
@Entity
@Table(name = "maintenance_records")
@NoArgsConstructor
public class MaintenanceRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 20)
    private MaintenanceType maintenanceType;

    @Column(name = "performed_at", nullable = false)
    private LocalDate performedAt;

    @Column(name = "mileage_at_service")
    private Integer mileageAtService;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "supplier", length = 200)
    private String supplier;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "parts_replaced")
    private String partsReplaced;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "next_maintenance_mileage")
    private Integer nextMaintenanceMileage;

    @Column(name = "responsible_user", nullable = false, length = 100)
    private String responsibleUser;
}
