package pt.xavier.tms.hr.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.hr.entity.EmployeeFunction;

public interface EmployeeFunctionRepository extends JpaRepository<EmployeeFunction, UUID> {

    Optional<EmployeeFunction> findByCode(String code);

    boolean existsByCode(String code);

    Page<EmployeeFunction> findByIsActiveTrue(Pageable pageable);
}
