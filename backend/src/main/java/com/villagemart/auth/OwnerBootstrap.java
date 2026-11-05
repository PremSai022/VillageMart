package com.villagemart.auth;

import com.villagemart.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Creates the single owner account from environment variables on first start-up (optional: when they are not set,
 * the owner can register once through {@code POST /api/v1/auth/register}). Also performs operator-driven password
 * recovery when {@code OWNER_RESET_PASSWORD} is set.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class OwnerBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Owner owner = properties.owner();
        if (userRepository.count() > 0) {
            resetPasswordIfRequested(owner);
            return;
        }
        if (owner == null || isBlank(owner.username()) || isBlank(owner.password())) {
            log.info("No owner account exists yet - owner registration is open via the app");
            return;
        }
        if (owner.password().length() < 8) {
            throw new IllegalStateException("OWNER_PASSWORD must be at least 8 characters");
        }
        User user = new User();
        user.setUsername(owner.username().trim());
        user.setMobile(isBlank(owner.mobile()) ? null : owner.mobile().trim());
        user.setFullName(isBlank(owner.name()) ? "Owner" : owner.name().trim());
        user.setPasswordHash(passwordEncoder.encode(owner.password()));
        user.setRole(Role.OWNER);
        user.setActive(true);
        userRepository.save(user);
        log.info("Initial owner account '{}' created", user.getUsername());
    }

    private void resetPasswordIfRequested(AppProperties.Owner owner) {
        if (owner == null || isBlank(owner.resetPassword())) {
            return;
        }
        if (!owner.resetPassword().matches(AuthDtos.PASSWORD_PATTERN)) {
            throw new IllegalStateException(
                    "OWNER_RESET_PASSWORD must be 8-100 characters and include a letter and a number");
        }
        User user = userRepository.findFirstByRoleOrderByIdAsc(Role.OWNER).orElse(null);
        if (user == null || passwordEncoder.matches(owner.resetPassword(), user.getPasswordHash())) {
            log.warn("OWNER_RESET_PASSWORD is still set - remove it from the environment now that recovery is complete");
            return;
        }
        user.setPasswordHash(passwordEncoder.encode(owner.resetPassword()));
        user.setActive(true);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        log.warn("Owner password was reset via OWNER_RESET_PASSWORD and all sessions were revoked. "
                + "Remove OWNER_RESET_PASSWORD from the environment.");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
