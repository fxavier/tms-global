package pt.xavier.tms.driver.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.driver.entity.DriverDocument;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;

public interface DriverDocumentRepository extends JpaRepository<DriverDocument, UUID> {

    List<DriverDocument> findByDriverId(UUID driverId);

    List<DriverDocument> findByDriverIdAndDocumentType(UUID driverId, DriverDocumentType type);

    List<DriverDocument> findByExpiryDateBetweenAndStatusNot(LocalDate from, LocalDate to, DocumentStatus status);

    List<DriverDocument> findByExpiryDateBeforeAndStatusNot(LocalDate date, DocumentStatus status);
}
