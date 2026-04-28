package pt.xavier.tms.security.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
@ConditionalOnProperty(name = "tms.jpa.auditing.enabled", havingValue = "true", matchIfMissing = true)
public class JpaAuditingConfig {

    static final String SYSTEM_AUDITOR = "system";

    @Bean
    AuditorAware<String> securityAuditorAware() {
        return () -> Optional.of(resolveCurrentAuditor());
    }

    private static String resolveCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_AUDITOR;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getSubject();
        }

        return authentication.getName();
    }
}
