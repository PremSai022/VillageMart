package com.villagemart.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :hash")
    Optional<RefreshToken> lockByTokenHash(String hash);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true, t.revokedAt = :now where t.user.id = :userId and t.revoked = false")
    int revokeAllForUser(Long userId, Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpired(Instant before);
}
