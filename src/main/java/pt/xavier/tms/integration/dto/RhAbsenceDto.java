package pt.xavier.tms.integration.dto;

import java.time.LocalDate;

public record RhAbsenceDto(
        LocalDate startDate,
        LocalDate endDate,
        String type,
        String reason
) {
}
