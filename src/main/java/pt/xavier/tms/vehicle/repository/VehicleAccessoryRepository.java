package pt.xavier.tms.vehicle.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.vehicle.entity.VehicleAccessory;

public interface VehicleAccessoryRepository extends JpaRepository<VehicleAccessory, UUID> {

    List<VehicleAccessory> findByVehicleId(UUID vehicleId);
}
