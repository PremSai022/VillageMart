package com.villagemart.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username or mobile is required") @Size(max = 60) String login,
            @NotBlank(message = "Password is required") @Size(max = 100) String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be 8-100 characters") String newPassword) {
    }

    public record UserDto(Long id, String username, String mobile, String fullName, String role) {
    }

    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, UserDto user) {
    }
}
