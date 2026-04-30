package pt.xavier.tms.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeePaymentStatusDto(
        UUID employeeId,
        String employeeNumber,
        String fullName,
        String functionName,
        int periodYear,
        int periodMonth,
        String paymentStatus,
        BigDecimal paidAmount,
        LocalDate paymentDate,
        String paymentReference
) {
}
