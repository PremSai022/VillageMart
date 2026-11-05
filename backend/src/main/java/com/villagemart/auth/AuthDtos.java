package com.villagemart.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    /** At least 8 characters with one letter and one digit. */
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$";
    /** Indian 10-digit mobile number. */
    public static final String MOBILE_PATTERN = "^[6-9]\\d{9}$";

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username or mobile is required") @Size(max = 120) String login,
            @NotBlank(message = "Password is required") @Size(max = 100) String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120, message = "Full name must be 2-120 characters") String fullName,
            @NotBlank(message = "Mobile number is required")
            @Pattern(regexp = MOBILE_PATTERN, message = "Enter a valid 10-digit mobile number") String mobileNumber,
            @Email(message = "Enter a valid email address") @Size(max = 120) String email,
            @NotBlank(message = "Password is required")
            @Pattern(regexp = PASSWORD_PATTERN,
                    message = "Password must be 8-100 characters and include a letter and a number") String password,
            @NotBlank(message = "Business name is required")
            @Size(max = 150, message = "Business name must be at most 150 characters") String businessName,
            @Size(max = 300, message = "Business address must be at most 300 characters") String businessAddress,
            @AssertTrue(message = "You must accept the terms and conditions") boolean acceptTerms) {
    }

    public record RegistrationStatus(boolean registrationOpen) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be 8-100 characters") String newPassword) {
    }

    public record UserDto(Long id, String username, String mobile, String email, String fullName, String role) {
    }

    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, UserDto user) {
    }
}
