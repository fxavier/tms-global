package pt.xavier.tms.hr.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.hr.dto.EmployeeCreateDto;
import pt.xavier.tms.hr.dto.EmployeeUpdateDto;
import pt.xavier.tms.hr.entity.Employee;
import pt.xavier.tms.hr.entity.EmployeeFunction;
import pt.xavier.tms.hr.repository.EmployeeFunctionRepository;
import pt.xavier.tms.hr.repository.EmployeeRepository;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.enums.EmployeeStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Service
@ConditionalOnProperty(name = "tms.hr.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFunctionRepository functionRepository;

    public EmployeeService(EmployeeRepository employeeRepository, EmployeeFunctionRepository functionRepository) {
        this.employeeRepository = employeeRepository;
        this.functionRepository = functionRepository;
    }

    @Auditable(entityType = "EMPLOYEE", operation = AuditOperation.CRIACAO)
    @Transactional
    public Employee createEmployee(EmployeeCreateDto dto) {
        validateUniqueOnCreate(dto.employeeNumber(), dto.idNumber());
        Employee employee = new Employee();
        employee.setEmployeeNumber(dto.employeeNumber());
        applyUpdatableFields(employee, dto.fullName(), dto.phone(), dto.email(), dto.idNumber(), dto.functionId(), dto.hireDate(),
                dto.terminationDate(), dto.baseSalary(), dto.currency(), dto.notes());
        employee.setStatus(dto.status() == null ? EmployeeStatus.ACTIVE : dto.status());
        return employeeRepository.save(employee);
    }

    @Auditable(entityType = "EMPLOYEE", operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public Employee updateEmployee(UUID id, EmployeeUpdateDto dto) {
        Employee employee = getEmployee(id);
        validateUniqueOnUpdate(employee, dto.idNumber());
        applyUpdatableFields(employee, dto.fullName(), dto.phone(), dto.email(), dto.idNumber(), dto.functionId(), dto.hireDate(),
                dto.terminationDate(), dto.baseSalary(), dto.currency(), dto.notes());
        return employee;
    }

    @Auditable(entityType = "EMPLOYEE", operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public Employee updateStatus(UUID id, EmployeeStatus status) {
        Employee employee = getEmployee(id);
        employee.setStatus(status);
        return employee;
    }

    @Auditable(entityType = "EMPLOYEE", operation = AuditOperation.ELIMINACAO)
    @Transactional
    public void deleteEmployee(UUID id) {
        getEmployee(id).softDelete("system");
    }

    public Employee getEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EMPLOYEE_NOT_FOUND", "Employee not found"));
    }

    public Page<Employee> listEmployees(EmployeeStatus status, UUID functionId, String q, Pageable pageable) {
        return employeeRepository.findAllByFilters(status, functionId, blankToNull(q), pageable);
    }

    private void validateUniqueOnCreate(String employeeNumber, String idNumber) {
        if (employeeRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new BusinessException("DUPLICATE_EMPLOYEE_NUMBER", "Employee number already exists");
        }
        if (idNumber != null && !idNumber.isBlank() && employeeRepository.existsByIdNumber(idNumber)) {
            throw new BusinessException("DUPLICATE_EMPLOYEE_ID_NUMBER", "Employee id number already exists");
        }
    }

    private void validateUniqueOnUpdate(Employee employee, String idNumber) {
        if (idNumber != null && !idNumber.isBlank() && employeeRepository.existsByIdNumberAndIdNot(idNumber, employee.getId())) {
            throw new BusinessException("DUPLICATE_EMPLOYEE_ID_NUMBER", "Employee id number already exists");
        }
    }

    private void applyUpdatableFields(
            Employee employee,
            String fullName,
            String phone,
            String email,
            String idNumber,
            UUID functionId,
            java.time.LocalDate hireDate,
            java.time.LocalDate terminationDate,
            java.math.BigDecimal baseSalary,
            String currency,
            String notes
    ) {
        employee.setFullName(fullName);
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setIdNumber(blankToNull(idNumber));
        employee.setFunction(resolveFunction(functionId));
        employee.setHireDate(hireDate);
        employee.setTerminationDate(terminationDate);
        employee.setBaseSalary(baseSalary);
        employee.setCurrency(currency == null || currency.isBlank() ? "MZN" : currency);
        employee.setNotes(notes);
    }

    private EmployeeFunction resolveFunction(UUID functionId) {
        if (functionId == null) {
            return null;
        }
        return functionRepository.findById(functionId)
                .orElseThrow(() -> new ResourceNotFoundException("EMPLOYEE_FUNCTION_NOT_FOUND", "Employee function not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
