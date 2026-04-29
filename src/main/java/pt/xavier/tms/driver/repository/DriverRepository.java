package pt.xavier.tms.driver.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.shared.enums.DriverStatus;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByIdNumber(String idNumber);

    boolean existsByIdNumber(String idNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("""
            select d
            from Driver d
            where (:status is null or d.status = :status)
              and (:location is null or lower(d.activityLocation) like lower(concat('%', :location, '%')))
            """)
    Page<Driver> findAllByFilters(
            @Param("status") DriverStatus status,
            @Param("location") String location,
            Pageable pageable
    );
}
