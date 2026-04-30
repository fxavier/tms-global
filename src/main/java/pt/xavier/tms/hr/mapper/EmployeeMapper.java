package pt.xavier.tms.hr.mapper;

import org.springframework.stereotype.Component;

import pt.xavier.tms.hr.dto.EmployeeResponseDto;
import pt.xavier.tms.hr.entity.Employee;

@Component
public class EmployeeMapper {

    public EmployeeResponseDto toResponse(Employee entity) {
        return new EmployeeResponseDto(
                entity.getId(),
                entity.getEmployeeNumber(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getIdNumber(),
                entity.getFunction() != null ? entity.getFunction().getId() : null,
                entity.getFunction() != null ? entity.getFunction().getName() : null,
                entity.getStatus(),
                entity.getHireDate(),
                entity.getTerminationDate(),
                entity.getBaseSalary(),
                entity.getCurrency(),
                entity.getNotes()
        );
    }
}
