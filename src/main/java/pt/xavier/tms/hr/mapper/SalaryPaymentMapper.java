package pt.xavier.tms.hr.mapper;

import org.springframework.stereotype.Component;

import pt.xavier.tms.hr.dto.SalaryPaymentResponseDto;
import pt.xavier.tms.hr.entity.SalaryPayment;

@Component
public class SalaryPaymentMapper {

    public SalaryPaymentResponseDto toResponse(SalaryPayment entity) {
        return new SalaryPaymentResponseDto(
                entity.getId(),
                entity.getEmployee().getId(),
                entity.getEmployee().getEmployeeNumber(),
                entity.getEmployee().getFullName(),
                entity.getPeriodYear(),
                entity.getPeriodMonth(),
                entity.getGrossAmount(),
                entity.getNetAmount(),
                entity.getPaidAmount(),
                entity.getCurrency(),
                entity.getPaymentDate(),
                entity.getPaymentMethod(),
                entity.getReference(),
                entity.getStatus(),
                entity.getNotes()
        );
    }
}
