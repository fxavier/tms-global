package pt.xavier.tms.vehicle.entity;

import java.time.Instant;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pt.xavier.tms.shared.enums.AccessoryStatus;
import pt.xavier.tms.shared.enums.AccessoryType;

@Getter
@Setter
@Entity
@Table(
        name = "vehicle_accessories",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehicle_accessories_vehicle_type", columnNames = {"vehicle_id", "accessory_type"})
)
@NoArgsConstructor
public class VehicleAccessory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "accessory_type", nullable = false, length = 50)
    private AccessoryType accessoryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccessoryStatus status = AccessoryStatus.PRESENTE;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_checked_by", length = 100)
    private String lastCheckedBy;

    @Column(name = "notes")
    private String notes;
}
