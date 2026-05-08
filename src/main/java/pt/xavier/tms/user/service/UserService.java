package pt.xavier.tms.user.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import pt.xavier.tms.audit.event.AuditEvent;
import pt.xavier.tms.security.util.SecurityUtils;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.user.dto.UserCreateDto;
import pt.xavier.tms.user.dto.UserResponseDto;
import pt.xavier.tms.user.dto.UserUpdateDto;

@Service
@ConditionalOnProperty(name = "tms.user.services.enabled", havingValue = "true", matchIfMissing = true)
public class UserService {

    private static final String USER_ENTITY = "USER";

    private final RealmResource realmResource;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserService(RealmResource realmResource, ApplicationEventPublisher applicationEventPublisher) {
        this.realmResource = realmResource;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public UserResponseDto createUser(UserCreateDto dto) {
        validateUniqueUsernameAndEmail(dto.username(), dto.email(), null);
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(dto.username());
        representation.setEmail(dto.email());
        representation.setFirstName(dto.firstName());
        representation.setLastName(dto.lastName());
        representation.setEnabled(dto.enabled());
        representation.setEmailVerified(false);

        String userId;
        try (Response response = realmResource.users().create(representation)) {
            if (response.getStatus() >= 400) {
                throw new BusinessException("USER_CREATE_FAILED", "Failed to create user in Keycloak");
            }
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        syncRoles(userId, dto.roles());
        realmResource.users().get(userId).executeActionsEmail(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));
        publishAudit(AuditOperation.CRIACAO, userId, null, Map.of("username", dto.username(), "email", dto.email()));
        return getUser(userId);
    }

    public Page<UserResponseDto> listUsers(String role, Boolean enabled, String q, Pageable pageable) {
        List<UserRepresentation> users = searchUsers(q);
        List<UserResponseDto> filtered = users.stream()
                .map(this::toResponse)
                .filter(user -> enabled == null || user.enabled() == enabled)
                .filter(user -> role == null || user.roles().contains(role))
                .toList();

        int from = (int) Math.min(pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(from, to), pageable, filtered.size());
    }

    public UserResponseDto getUser(String userId) {
        try {
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            return toResponse(user);
        } catch (NotFoundException ex) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found");
        }
    }

    public UserResponseDto updateUser(String userId, UserUpdateDto dto) {
        UserRepresentation current = realmResource.users().get(userId).toRepresentation();
        validateUniqueUsernameAndEmail(current.getUsername(), dto.email(), userId);
        if (dto.roles().contains("SUPERUSER") && SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("SUPERUSER")) {
            throw new BusinessException("SUPERUSER_ASSIGN_FORBIDDEN", "ADMIN cannot assign SUPERUSER role");
        }

        Map<String, Object> previous = snapshot(current, getRoles(userId));
        current.setEmail(dto.email());
        current.setFirstName(dto.firstName());
        current.setLastName(dto.lastName());
        current.setEnabled(dto.enabled());
        realmResource.users().get(userId).update(current);
        syncRoles(userId, dto.roles());

        UserResponseDto updated = getUser(userId);
        publishAudit(AuditOperation.ATUALIZACAO, userId, previous, snapshot(updated));
        return updated;
    }

    public UserResponseDto setUserEnabled(String userId, boolean enabled) {
        String currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (!enabled && Objects.equals(currentUserId, userId)) {
            throw new BusinessException("SELF_DISABLE_FORBIDDEN", "User cannot disable own account");
        }

        UserRepresentation current = realmResource.users().get(userId).toRepresentation();
        Map<String, Object> previous = snapshot(current, getRoles(userId));
        current.setEnabled(enabled);
        realmResource.users().get(userId).update(current);
        if (!enabled) {
            realmResource.users().get(userId).logout();
        }
        UserResponseDto updated = getUser(userId);
        publishAudit(AuditOperation.ATUALIZACAO, userId, previous, snapshot(updated));
        return updated;
    }

    public void forcePasswordReset(String userId) {
        realmResource.users().get(userId).executeActionsEmail(List.of("UPDATE_PASSWORD"));
        publishAudit(AuditOperation.ATUALIZACAO, userId, null, Map.of("action", "FORCE_PASSWORD_RESET"));
    }

    public UserResponseDto getMe(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Set<String> roles = new HashSet<>();
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roleList) {
            for (Object role : roleList) {
                roles.add(String.valueOf(role));
            }
        }

        return new UserResponseDto(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                Set.copyOf(roles),
                true,
                null
        );
    }

    private List<UserRepresentation> searchUsers(String q) {
        if (q == null || q.isBlank()) {
            return realmResource.users().list();
        }
        return realmResource.users().search(q.trim(), true);
    }

    private void validateUniqueUsernameAndEmail(String username, String email, String excludeUserId) {
        List<UserRepresentation> users = realmResource.users().search(username, true);
        for (UserRepresentation user : users) {
            if (username.equalsIgnoreCase(user.getUsername()) && !Objects.equals(user.getId(), excludeUserId)) {
                throw new BusinessException("DUPLICATE_USERNAME", "Username already exists");
            }
        }

        List<UserRepresentation> byEmail = realmResource.users().searchByEmail(email, true);
        for (UserRepresentation user : byEmail) {
            if (email.equalsIgnoreCase(user.getEmail()) && !Objects.equals(user.getId(), excludeUserId)) {
                throw new BusinessException("DUPLICATE_EMAIL", "Email already exists");
            }
        }
    }

    private UserResponseDto toResponse(UserRepresentation user) {
        Set<String> roles = getRoles(user.getId());
        Instant createdAt = user.getCreatedTimestamp() == null ? null : Instant.ofEpochMilli(user.getCreatedTimestamp());
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                Boolean.TRUE.equals(user.isEnabled()),
                createdAt
        );
    }

    private Set<String> getRoles(String userId) {
        RoleMappingResource roleMappings = realmResource.users().get(userId).roles();
        List<RoleRepresentation> realmRoles = roleMappings.realmLevel().listEffective();
        return realmRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private void syncRoles(String userId, Set<String> roleNames) {
        var realmLevel = realmResource.users().get(userId).roles().realmLevel();
        List<RoleRepresentation> existing = realmLevel.listAll();
        if (!existing.isEmpty()) {
            realmLevel.remove(existing);
        }

        List<RoleRepresentation> target = new ArrayList<>();
        for (String roleName : roleNames) {
            target.add(realmResource.roles().get(roleName).toRepresentation());
        }
        if (!target.isEmpty()) {
            realmLevel.add(target);
        }
    }

    private void publishAudit(AuditOperation operation, String userId, Map<String, Object> previous, Map<String, Object> next) {
        applicationEventPublisher.publishEvent(AuditEvent.of(
                USER_ENTITY,
                safeUuid(userId),
                operation,
                SecurityUtils.getCurrentUserId().orElse("system"),
                SecurityUtils.getCurrentIpAddress(),
                previous,
                next
        ));
    }

    private static UUID safeUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Map<String, Object> snapshot(UserRepresentation user, Set<String> roles) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("enabled", user.isEnabled());
        data.put("roles", roles);
        return data;
    }

    private static Map<String, Object> snapshot(UserResponseDto user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.id());
        data.put("username", user.username());
        data.put("email", user.email());
        data.put("firstName", user.firstName());
        data.put("lastName", user.lastName());
        data.put("enabled", user.enabled());
        data.put("roles", user.roles());
        return data;
    }
}
