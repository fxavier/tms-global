package pt.xavier.tms.security.filter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.xavier.tms.security.config.RateLimitConfig;
import pt.xavier.tms.security.util.SecurityUtils;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RateLimitConfig config;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!config.enabled()) {
            return true;
        }

        String path = request.getRequestURI();
        return !(path.startsWith("/api/v1/integration/")
                || path.equals("/actuator")
                || path.startsWith("/actuator/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = SecurityUtils.getClientIpAddress(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), rateLimitResponse());
    }

    private Bucket newBucket() {
        Refill refill = Refill.intervally(
                config.refillTokens(),
                Duration.ofSeconds(config.refillPeriodSeconds())
        );
        Bandwidth limit = Bandwidth.classic(config.capacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private static Map<String, Object> rateLimitResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "RATE_LIMIT_EXCEEDED");
        error.put("message", "Too many requests. Please retry later.");
        error.put("timestamp", Instant.now().toString());
        response.put("data", null);
        response.put("error", error);
        return response;
    }
}
