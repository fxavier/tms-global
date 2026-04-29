package pt.xavier.tms.security.jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject());
    }

    private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object realmAccessClaim = jwt.getClaim("realm_access");

        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return Collections.emptyList();
        }

        Object rolesClaim = realmAccess.get("roles");

        if (!(rolesClaim instanceof Collection<?> roles)) {
            return Collections.emptyList();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(KeycloakJwtAuthenticationConverter::toRoleAuthority)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private static Stream<String> toRoleAuthority(String role) {
        if (role == null || role.isBlank()) {
            return Stream.empty();
        }

        String normalizedRole = role.trim();
        return Stream.of(normalizedRole.startsWith(ROLE_PREFIX) ? normalizedRole : ROLE_PREFIX + normalizedRole);
    }
}
