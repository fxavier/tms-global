package pt.xavier.tms.integration.rh;

import java.time.LocalDate;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import pt.xavier.tms.integration.config.RhIntegrationConfig;
import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.integration.exception.RhIntegrationException;
import pt.xavier.tms.integration.port.RhIntegrationPort;

@Component
@ConditionalOnProperty(name = "tms.integration.rh.mode", havingValue = "rest")
public class RhRestAdapter implements RhIntegrationPort {

    private final RestTemplate restTemplate;
    private final RhIntegrationConfig config;
    private final Cache<String, DriverAvailabilityDto> cache;

    public RhRestAdapter(
            RestTemplate rhRestTemplate,
            RhIntegrationConfig config,
            Cache<String, DriverAvailabilityDto> rhAvailabilityCache
    ) {
        this.restTemplate = rhRestTemplate;
        this.config = config;
        this.cache = rhAvailabilityCache;
    }

    @Override
    public DriverAvailabilityDto checkAvailability(UUID driverId, LocalDate startDate, LocalDate endDate) {
        String cacheKey = cacheKey(driverId, startDate, endDate);
        DriverAvailabilityDto cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String uri = UriComponentsBuilder.fromPath(config.getAvailabilityPath())
                    .queryParam("driverId", driverId)
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                    .build()
                    .toUriString();

            DriverAvailabilityDto response = restTemplate.getForObject(uri, DriverAvailabilityDto.class);
            if (response == null) {
                throw new RhIntegrationException("RH availability response is empty", null);
            }

            cache.put(cacheKey, response);
            return response;
        } catch (RestClientException ex) {
            throw new RhIntegrationException("Failed to fetch driver availability from RH service", ex);
        }
    }

    private String cacheKey(UUID driverId, LocalDate startDate, LocalDate endDate) {
        return driverId + ":" + startDate + ":" + endDate;
    }
}
