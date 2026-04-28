package pt.xavier.tms.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JpaAuditingConfigTests {

    private final AuditorAware<String> auditorAware = new JpaAuditingConfig().securityAuditorAware();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSystemAuditorWhenAuthenticationIsMissing() {
        assertThat(auditorAware.getCurrentAuditor()).contains(JpaAuditingConfig.SYSTEM_AUDITOR);
    }

    @Test
    void returnsAuthenticatedUserName() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-123",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        assertThat(auditorAware.getCurrentAuditor()).contains("user-123");
    }
}
