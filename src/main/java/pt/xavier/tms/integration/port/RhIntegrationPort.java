package pt.xavier.tms.integration.port;

import java.time.LocalDate;
import java.util.UUID;

import pt.xavier.tms.integration.dto.DriverAvailabilityDto;

public interface RhIntegrationPort {

    DriverAvailabilityDto checkAvailability(UUID driverId, LocalDate startDate, LocalDate endDate);
}
