package pt.xavier.tms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestDto(
        @NotBlank @Size(min = 8, max = 100) String temporaryPassword
) {
}
