package pt.xavier.tms.integration.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import pt.xavier.tms.integration.dto.DriverAvailabilityDto;

@Configuration
public class RhClientConfiguration {

    @Bean
    Cache<String, DriverAvailabilityDto> rhAvailabilityCache(RhIntegrationConfig config) {
        return Caffeine.newBuilder()
                .expireAfterWrite(config.getCacheTtlMinutes(), TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "tms.integration.rh.mode", havingValue = "rest")
    RestTemplate rhRestTemplate(RestTemplateBuilder builder, RhIntegrationConfig config) {
        RestTemplateBuilder configuredBuilder = builder
                .rootUri(config.getBaseUrl())
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis()))
                .readTimeout(Duration.ofMillis(config.getReadTimeoutMillis()));

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            configuredBuilder = configuredBuilder.additionalInterceptors((request, body, execution) -> {
                request.getHeaders().add(HttpHeaders.AUTHORIZATION, "ApiKey " + config.getApiKey());
                return execution.execute(request, body);
            });
        }

        return configuredBuilder.build();
    }
}
