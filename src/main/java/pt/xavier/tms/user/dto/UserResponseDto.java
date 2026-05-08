package pt.xavier.tms.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponseDto(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        boolean enabled,
        Instant createdAt
) {
}
