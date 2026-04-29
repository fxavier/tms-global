package pt.xavier.tms.integration.rh;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.integration.port.RhIntegrationPort;

@Component
@ConditionalOnProperty(name = "tms.integration.rh.mode", havingValue = "module", matchIfMissing = true)
public class RhModuleAdapter implements RhIntegrationPort {

    @Override
    public DriverAvailabilityDto checkAvailability(UUID driverId, LocalDate startDate, LocalDate endDate) {
        return new DriverAvailabilityDto(driverId, true, null, List.of());
    }
}
