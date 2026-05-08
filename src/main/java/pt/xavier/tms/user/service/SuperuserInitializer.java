package pt.xavier.tms.user.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import pt.xavier.tms.audit.event.AuditEvent;
import pt.xavier.tms.security.util.SecurityUtils;
import pt.xavier.tms.shared.enums.AuditOperation;

@Component
@ConditionalOnProperty(name = "tms.user.superuser-init.enabled", havingValue = "true")
public class SuperuserInitializer {

    private final RealmResource realmResource;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String username;
    private final String password;
    private final String email;

    public SuperuserInitializer(
            RealmResource realmResource,
            ApplicationEventPublisher applicationEventPublisher,
            @Value("${tms.superuser.username:}") String username,
            @Value("${tms.superuser.password:}") String password,
            @Value("${tms.superuser.email:}") String email
    ) {
        this.realmResource = realmResource;
        this.applicationEventPublisher = applicationEventPublisher;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @PostConstruct
    void initialize() {
        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            return;
        }

        List<UserRepresentation> users = realmResource.users().search(username, true);
        if (users.stream().anyMatch(u -> username.equalsIgnoreCase(u.getUsername()))) {
            return;
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);

        String userId;
        try (Response response = realmResource.users().create(user)) {
            if (response.getStatus() >= 400) {
                return;
            }
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        realmResource.users().get(userId).resetPassword(credential);

        realmResource.users().get(userId).roles().realmLevel().add(List.of(realmResource.roles().get("SUPERUSER").toRepresentation()));

        applicationEventPublisher.publishEvent(AuditEvent.of(
                "USER",
                safeUuid(userId),
                AuditOperation.CRIACAO,
                "system",
                SecurityUtils.getCurrentIpAddress(),
                null,
                Map.of("username", username, "email", email, "roles", List.of("SUPERUSER"))
        ));
    }

    private static UUID safeUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
