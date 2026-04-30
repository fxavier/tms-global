package pt.xavier.tms.hr.mapper;

import org.springframework.stereotype.Component;

import pt.xavier.tms.hr.dto.EmployeeFunctionResponseDto;
import pt.xavier.tms.hr.entity.EmployeeFunction;

@Component
public class EmployeeFunctionMapper {

    public EmployeeFunctionResponseDto toResponse(EmployeeFunction entity) {
        return new EmployeeFunctionResponseDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive()
        );
    }
}
