package pt.xavier.tms.vehicle.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.vehicle.entity.ChecklistInspection;

public interface ChecklistInspectionRepository extends JpaRepository<ChecklistInspection, UUID> {

    default Optional<ChecklistInspection> findLatestByVehicleId(UUID vehicleId) {
        return findTopByVehicleIdOrderByPerformedAtDesc(vehicleId);
    }

    Optional<ChecklistInspection> findTopByVehicleIdOrderByPerformedAtDesc(UUID vehicleId);

    Page<ChecklistInspection> findByVehicleId(UUID vehicleId, Pageable pageable);
}
