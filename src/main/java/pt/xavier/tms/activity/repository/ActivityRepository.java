package pt.xavier.tms.activity.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import pt.xavier.tms.activity.entity.Activity;
import pt.xavier.tms.shared.enums.ActivityStatus;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Optional<Activity> findByCode(String code);

    long countByCodeStartingWith(String prefix);

    @Query("""
            select a
            from Activity a
            where (:status is null or a.status = :status)
              and (:vehicleId is null or a.vehicle.id = :vehicleId)
              and (:driverId is null or a.driver.id = :driverId)
              and (:from is null or a.plannedStart >= :from)
              and (:to is null or a.plannedEnd <= :to)
            """)
    Page<Activity> findAllByFilters(
            @Param("status") ActivityStatus status,
            @Param("vehicleId") UUID vehicleId,
            @Param("driverId") UUID driverId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Activity a
            where a.vehicle.id = :vehicleId
              and a.status in ('PLANEADA', 'EM_CURSO')
              and a.deletedAt is null
              and a.plannedStart < :end
              and a.plannedEnd > :start
              and (:excludeActivityId is null or a.id <> :excludeActivityId)
            """)
    List<Activity> findConflictingActivitiesForVehicle(
            @Param("vehicleId") UUID vehicleId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("excludeActivityId") UUID excludeActivityId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Activity a
            where a.driver.id = :driverId
              and a.status in ('PLANEADA', 'EM_CURSO')
              and a.deletedAt is null
              and a.plannedStart < :end
              and a.plannedEnd > :start
              and (:excludeActivityId is null or a.id <> :excludeActivityId)
            """)
    List<Activity> findConflictingActivitiesForDriver(
            @Param("driverId") UUID driverId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("excludeActivityId") UUID excludeActivityId
    );
}
