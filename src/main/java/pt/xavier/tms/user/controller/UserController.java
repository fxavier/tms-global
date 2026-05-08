package pt.xavier.tms.user.controller;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.user.dto.PasswordResetRequestDto;
import pt.xavier.tms.user.dto.UserCreateDto;
import pt.xavier.tms.user.dto.UserResponseDto;
import pt.xavier.tms.user.dto.UserUpdateDto;
import pt.xavier.tms.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@ConditionalOnProperty(name = "tms.user.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(@Valid @RequestBody UserCreateDto request) {
        return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ApiResponse<PagedResponse<UserResponseDto>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(userService.listUsers(role, enabled, q, Pageables.of(page, size))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ApiResponse<UserResponseDto> getUser(@PathVariable String id) {
        return ApiResponse.success(userService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ApiResponse<UserResponseDto> updateUser(@PathVariable String id, @Valid @RequestBody UserUpdateDto request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ApiResponse<UserResponseDto> enableUser(@PathVariable String id) {
        return ApiResponse.success(userService.setUserEnabled(id, true));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ApiResponse<UserResponseDto> disableUser(@PathVariable String id) {
        return ApiResponse.success(userService.setUserEnabled(id, false));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERUSER')")
    public ApiResponse<Void> resetPassword(@PathVariable String id, @Valid @RequestBody PasswordResetRequestDto request) {
        userService.forcePasswordReset(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponseDto> getMe(JwtAuthenticationToken authentication) {
        return ApiResponse.success(userService.getMe(authentication));
    }
}
