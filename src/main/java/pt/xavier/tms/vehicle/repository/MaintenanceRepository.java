package pt.xavier.tms.vehicle.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.vehicle.entity.MaintenanceRecord;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {

    Page<MaintenanceRecord> findByVehicleId(UUID vehicleId, Pageable pageable);

    List<MaintenanceRecord> findByNextMaintenanceDateBetween(LocalDate from, LocalDate to);

    List<MaintenanceRecord> findByNextMaintenanceDateBefore(LocalDate date);
}
