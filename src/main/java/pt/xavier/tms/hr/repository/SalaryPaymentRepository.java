package pt.xavier.tms.hr.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.xavier.tms.hr.entity.SalaryPayment;
import pt.xavier.tms.shared.enums.SalaryPaymentStatus;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, UUID> {

    Page<SalaryPayment> findByEmployeeId(UUID employeeId, Pageable pageable);

    Page<SalaryPayment> findByPeriodYearAndPeriodMonth(int year, int month, Pageable pageable);

    boolean existsByEmployeeIdAndPeriodYearAndPeriodMonth(UUID employeeId, int year, int month);

    @Query("""
            select sp.employee.id
            from SalaryPayment sp
            where sp.periodYear = :year
              and sp.periodMonth = :month
              and sp.status = :status
            """)
    List<UUID> findPaidEmployeeIdsByPeriod(
            @Param("year") int year,
            @Param("month") int month,
            @Param("status") SalaryPaymentStatus status
    );
}
