package pt.xavier.tms.user.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "tms.user.services.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    @Bean
    Keycloak keycloakAdminClient(KeycloakAdminProperties properties) {
        KeycloakBuilder builder = KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getClientId())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS);

        if (properties.getClientSecret() != null && !properties.getClientSecret().isBlank()) {
            builder.clientSecret(properties.getClientSecret());
        }
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            builder.username(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            builder.password(properties.getPassword());
        }

        return builder.build();
    }

    @Bean
    RealmResource realmResource(Keycloak keycloakAdminClient, KeycloakAdminProperties properties) {
        return keycloakAdminClient.realm(properties.getRealm());
    }
}
