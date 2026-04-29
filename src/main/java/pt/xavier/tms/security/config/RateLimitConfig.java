package pt.xavier.tms.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tms.security.rate-limit")
public record RateLimitConfig(
        boolean enabled,
        long capacity,
        long refillTokens,
        long refillPeriodSeconds
) {

    public RateLimitConfig {
        if (capacity < 1) {
            capacity = 60;
        }
        if (refillTokens < 1) {
            refillTokens = capacity;
        }
        if (refillPeriodSeconds < 1) {
            refillPeriodSeconds = 60;
        }
    }
}
