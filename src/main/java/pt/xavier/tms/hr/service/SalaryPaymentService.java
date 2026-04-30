package pt.xavier.tms.hr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.hr.dto.EmployeePaymentStatusDto;
import pt.xavier.tms.hr.dto.PaymentStatusFilter;
import pt.xavier.tms.hr.dto.SalaryPaymentCreateDto;
import pt.xavier.tms.hr.entity.Employee;
import pt.xavier.tms.hr.entity.SalaryPayment;
import pt.xavier.tms.hr.repository.EmployeeRepository;
import pt.xavier.tms.hr.repository.SalaryPaymentRepository;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.enums.EmployeeStatus;
import pt.xavier.tms.shared.enums.SalaryPaymentStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Service
@ConditionalOnProperty(name = "tms.hr.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class SalaryPaymentService {

    private final SalaryPaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryPaymentService(SalaryPaymentRepository paymentRepository, EmployeeRepository employeeRepository) {
        this.paymentRepository = paymentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Auditable(entityType = "SALARY_PAYMENT", operation = AuditOperation.CRIACAO)
    @Transactional
    public SalaryPayment registerPayment(SalaryPaymentCreateDto dto) {
        if (paymentRepository.existsByEmployeeIdAndPeriodYearAndPeriodMonth(dto.employeeId(), dto.periodYear(), dto.periodMonth())) {
            throw new BusinessException("DUPLICATE_SALARY_PAYMENT", "Salary payment already registered for this period");
        }
        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("EMPLOYEE_NOT_FOUND", "Employee not found"));
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("EMPLOYEE_NOT_ACTIVE", "Employee must be ACTIVE to receive payment");
        }

        SalaryPayment payment = new SalaryPayment();
        payment.setEmployee(employee);
        payment.setPeriodYear(dto.periodYear());
        payment.setPeriodMonth(dto.periodMonth());
        payment.setGrossAmount(dto.grossAmount());
        payment.setNetAmount(dto.netAmount());
        payment.setPaidAmount(dto.paidAmount());
        payment.setCurrency(dto.currency() == null || dto.currency().isBlank() ? "MZN" : dto.currency());
        payment.setPaymentDate(dto.paymentDate());
        payment.setPaymentMethod(dto.paymentMethod());
        payment.setReference(dto.reference());
        payment.setNotes(dto.notes());
        payment.setStatus(SalaryPaymentStatus.PAID);
        return paymentRepository.save(payment);
    }

    @Auditable(entityType = "SALARY_PAYMENT", operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public SalaryPayment cancelPayment(UUID paymentId, String reason) {
        SalaryPayment payment = getPayment(paymentId);
        payment.setStatus(SalaryPaymentStatus.CANCELLED);
        payment.setNotes(reason);
        return payment;
    }

    public SalaryPayment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("SALARY_PAYMENT_NOT_FOUND", "Salary payment not found"));
    }

    public Page<SalaryPayment> listPayments(Integer year, Integer month, UUID employeeId, Pageable pageable) {
        if (employeeId != null) {
            return paymentRepository.findByEmployeeId(employeeId, pageable);
        }
        if (year != null && month != null) {
            return paymentRepository.findByPeriodYearAndPeriodMonth(year, month, pageable);
        }
        return paymentRepository.findAll(pageable);
    }

    public Page<EmployeePaymentStatusDto> getPaymentStatus(int year, int month, PaymentStatusFilter filter, Pageable pageable) {
        List<UUID> paidEmployeeIds = paymentRepository.findPaidEmployeeIdsByPeriod(year, month, SalaryPaymentStatus.PAID);
        Page<Employee> employees = employeeRepository.findAllByFilters(EmployeeStatus.ACTIVE, null, null, pageable);

        List<EmployeePaymentStatusDto> rows = new ArrayList<>();
        for (Employee employee : employees) {
            boolean paid = paidEmployeeIds.contains(employee.getId());
            if (filter == PaymentStatusFilter.PAID && !paid) {
                continue;
            }
            if (filter == PaymentStatusFilter.UNPAID && paid) {
                continue;
            }
            rows.add(new EmployeePaymentStatusDto(
                    employee.getId(),
                    employee.getEmployeeNumber(),
                    employee.getFullName(),
                    employee.getFunction() != null ? employee.getFunction().getName() : null,
                    year,
                    month,
                    paid ? "PAID" : "UNPAID",
                    null,
                    null,
                    null
            ));
        }

        return new org.springframework.data.domain.PageImpl<>(rows, pageable, employees.getTotalElements());
    }
}
