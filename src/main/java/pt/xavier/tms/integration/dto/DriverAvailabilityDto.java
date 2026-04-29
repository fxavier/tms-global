package pt.xavier.tms.integration.dto;

import java.util.List;
import java.util.UUID;

public record DriverAvailabilityDto(
        UUID driverId,
        boolean available,
        String reason,
        List<RhAbsenceDto> absences
) {
}
