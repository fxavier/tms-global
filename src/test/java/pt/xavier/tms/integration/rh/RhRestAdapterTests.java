package pt.xavier.tms.integration.rh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import pt.xavier.tms.integration.config.RhIntegrationConfig;
import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.integration.exception.RhIntegrationException;

@ExtendWith(MockitoExtension.class)
class RhRestAdapterTests {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void checkAvailabilityUsesCacheOnSecondCall() {
        UUID driverId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);
        Cache<String, DriverAvailabilityDto> cache = Caffeine.newBuilder().maximumSize(1000).build();
        RhRestAdapter adapter = new RhRestAdapter(restTemplate, config(), cache);
        DriverAvailabilityDto response = new DriverAvailabilityDto(driverId, true, null, List.of());

        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(DriverAvailabilityDto.class)))
                .thenReturn(response);

        DriverAvailabilityDto first = adapter.checkAvailability(driverId, startDate, endDate);
        DriverAvailabilityDto second = adapter.checkAvailability(driverId, startDate, endDate);

        assertThat(first).isEqualTo(response);
        assertThat(second).isEqualTo(response);
        verify(restTemplate, times(1))
                .getForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(DriverAvailabilityDto.class));
    }

    @Test
    void checkAvailabilityWrapsRestErrorsAsRhIntegrationException() {
        UUID driverId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);
        Cache<String, DriverAvailabilityDto> cache = Caffeine.newBuilder().maximumSize(1000).build();
        RhRestAdapter adapter = new RhRestAdapter(restTemplate, config(), cache);

        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(DriverAvailabilityDto.class)))
                .thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> adapter.checkAvailability(driverId, startDate, endDate))
                .isInstanceOf(RhIntegrationException.class)
                .hasMessage("Failed to fetch driver availability from RH service");
    }

    private static RhIntegrationConfig config() {
        RhIntegrationConfig config = new RhIntegrationConfig();
        config.setAvailabilityPath("/api/v1/drivers/availability");
        return config;
    }
}
