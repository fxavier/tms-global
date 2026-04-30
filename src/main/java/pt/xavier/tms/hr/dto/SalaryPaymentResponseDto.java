package pt.xavier.tms.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import pt.xavier.tms.shared.enums.PaymentMethod;
import pt.xavier.tms.shared.enums.SalaryPaymentStatus;

public record SalaryPaymentResponseDto(
        UUID id,
        UUID employeeId,
        String employeeNumber,
        String employeeName,
        int periodYear,
        int periodMonth,
        BigDecimal grossAmount,
        BigDecimal netAmount,
        BigDecimal paidAmount,
        String currency,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String reference,
        SalaryPaymentStatus status,
        String notes
) {
}
