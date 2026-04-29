package pt.xavier.tms.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import pt.xavier.tms.security.config.RateLimitConfig;

import java.io.IOException;

class RateLimitFilterTests {

    @Test
    void appliesRateLimitToIntegrationEndpointsByIp() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(new RateLimitConfig(true, 1, 1, 60), new ObjectMapper());

        MockHttpServletRequest firstRequest = request("/api/v1/integration/rh/availability");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = request("/api/v1/integration/rh/availability");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(secondResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
    }

    @Test
    void skipsNonProtectedEndpoints() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(new RateLimitConfig(true, 1, 1, 60), new ObjectMapper());

        MockHttpServletRequest firstRequest = request("/api/v1/vehicles");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = request("/api/v1/vehicles");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    private static MockHttpServletRequest request(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
