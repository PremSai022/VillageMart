package com.villagemart.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByActiveTrue();

    List<DeviceToken> findByTokenIn(Collection<String> tokens);
}
