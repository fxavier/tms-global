package pt.xavier.tms.hr.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.xavier.tms.hr.entity.Employee;
import pt.xavier.tms.shared.enums.EmployeeStatus;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByIdNumber(String idNumber);

    boolean existsByIdNumberAndIdNot(String idNumber, UUID id);

    @Query("""
            select e
            from Employee e
            where (:status is null or e.status = :status)
              and (:functionId is null or e.function.id = :functionId)
              and (:q is null or lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                   or lower(e.fullName) like lower(concat('%', :q, '%')))
            """)
    Page<Employee> findAllByFilters(
            @Param("status") EmployeeStatus status,
            @Param("functionId") UUID functionId,
            @Param("q") String q,
            Pageable pageable
    );
}
