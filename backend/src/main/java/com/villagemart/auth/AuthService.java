package com.villagemart.auth;

import com.villagemart.auth.AuthDtos.AuthResponse;
import com.villagemart.auth.AuthDtos.UserDto;
import com.villagemart.common.ApiException;
import com.villagemart.common.IdempotencyService;
import com.villagemart.config.AppProperties;
import com.villagemart.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Transactional
    public AuthResponse login(String login, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login.trim(), password));
        User user = userRepository.findByLogin(login.trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        user.setLastLoginAt(Instant.now());
        log.info("User {} logged in", user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository.lockByTokenHash(IdempotencyService.sha256(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (token.isRevoked()) {
            // Reuse of a rotated token indicates theft: revoke the whole token family for this user.
            refreshTokenRepository.revokeAllForUser(token.getUser().getId(), Instant.now());
            log.warn("Revoked refresh token reused for user {}", token.getUser().getUsername());
            throw ApiException.unauthorized("Refresh token has been revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Refresh token has expired");
        }
        User user = token.getUser();
        if (!user.isActive()) {
            throw ApiException.unauthorized("Account is disabled");
        }
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.lockByTokenHash(IdempotencyService.sha256(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return toDto(userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId)));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.business("INVALID_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void purgeExpiredTokens() {
        int removed = refreshTokenRepository.deleteExpired(Instant.now().minus(Duration.ofDays(1)));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }

    private AuthResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(IdempotencyService.sha256(raw));
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(properties.security().refreshTokenTtlDays())));
        refreshTokenRepository.save(token);
        String access = jwtService.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(access, raw, "Bearer", jwtService.accessTokenTtlSeconds(), toDto(user));
    }

    static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getMobile(), user.getFullName(), user.getRole().name());
    }
}
