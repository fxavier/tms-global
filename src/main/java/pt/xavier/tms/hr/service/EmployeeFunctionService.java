package pt.xavier.tms.hr.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.hr.dto.EmployeeFunctionCreateDto;
import pt.xavier.tms.hr.dto.EmployeeFunctionUpdateDto;
import pt.xavier.tms.hr.entity.EmployeeFunction;
import pt.xavier.tms.hr.repository.EmployeeFunctionRepository;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Service
@ConditionalOnProperty(name = "tms.hr.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class EmployeeFunctionService {

    private final EmployeeFunctionRepository repository;

    public EmployeeFunctionService(EmployeeFunctionRepository repository) {
        this.repository = repository;
    }

    @Auditable(entityType = "EMPLOYEE_FUNCTION", operation = AuditOperation.CRIACAO)
    @Transactional
    public EmployeeFunction createFunction(EmployeeFunctionCreateDto dto) {
        if (repository.existsByCode(dto.code())) {
            throw new BusinessException("DUPLICATE_EMPLOYEE_FUNCTION_CODE", "Employee function code already exists");
        }
        EmployeeFunction entity = new EmployeeFunction();
        entity.setCode(dto.code());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setActive(true);
        return repository.save(entity);
    }

    @Auditable(entityType = "EMPLOYEE_FUNCTION", operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public EmployeeFunction updateFunction(UUID id, EmployeeFunctionUpdateDto dto) {
        EmployeeFunction entity = getFunction(id);
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        return entity;
    }

    @Auditable(entityType = "EMPLOYEE_FUNCTION", operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public EmployeeFunction activate(UUID id) {
        EmployeeFunction entity = getFunction(id);
        entity.setActive(true);
        return entity;
    }

    @Auditable(entityType = "EMPLOYEE_FUNCTION", operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public EmployeeFunction deactivate(UUID id) {
        EmployeeFunction entity = getFunction(id);
        entity.setActive(false);
        return entity;
    }

    public Page<EmployeeFunction> listFunctions(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public EmployeeFunction getFunction(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EMPLOYEE_FUNCTION_NOT_FOUND", "Employee function not found"));
    }
}
