package pt.xavier.tms.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pt.xavier.tms.shared.enums.PaymentMethod;

public record SalaryPaymentCreateDto(
        @NotNull UUID employeeId,
        int periodYear,
        @Min(1) @Max(12) int periodMonth,
        @NotNull @Positive BigDecimal grossAmount,
        @NotNull @Positive BigDecimal netAmount,
        @NotNull @Positive BigDecimal paidAmount,
        @Size(max = 3) String currency,
        @NotNull LocalDate paymentDate,
        PaymentMethod paymentMethod,
        @Size(max = 100) String reference,
        String notes
) {
}
