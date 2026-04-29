package pt.xavier.tms.vehicle.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.vehicle.entity.VehicleDocument;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    List<VehicleDocument> findByVehicleId(UUID vehicleId);

    List<VehicleDocument> findByVehicleIdAndStatus(UUID vehicleId, DocumentStatus status);

    List<VehicleDocument> findByExpiryDateBetweenAndStatusNot(LocalDate from, LocalDate to, DocumentStatus status);

    List<VehicleDocument> findByExpiryDateBeforeAndStatusNot(LocalDate date, DocumentStatus status);
}
