package com.villagemart.auth;

import com.villagemart.auth.AuthDtos.AuthResponse;
import com.villagemart.auth.AuthDtos.ChangePasswordRequest;
import com.villagemart.auth.AuthDtos.LoginRequest;
import com.villagemart.auth.AuthDtos.LogoutRequest;
import com.villagemart.auth.AuthDtos.RefreshRequest;
import com.villagemart.auth.AuthDtos.RegisterRequest;
import com.villagemart.auth.AuthDtos.RegistrationStatus;
import com.villagemart.auth.AuthDtos.UserDto;
import com.villagemart.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.login(), request.password());
    }

    /** Public: tells clients whether the one-time owner registration is still available. */
    @GetMapping("/registration-status")
    public RegistrationStatus registrationStatus() {
        return new RegistrationStatus(authService.registrationOpen());
    }

    /** Public, but only succeeds while no owner account exists (single-owner business). */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me() {
        return authService.me(CurrentUser.require().id());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.require().id(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
