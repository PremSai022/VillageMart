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

/**
 * Creates the single owner account from environment variables on first start-up.
 * There is intentionally no public registration endpoint.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class OwnerBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        AppProperties.Owner owner = properties.owner();
        if (owner == null || isBlank(owner.username()) || isBlank(owner.password())) {
            log.warn("No users exist and OWNER_USERNAME/OWNER_PASSWORD are not set - nobody can log in until configured");
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
