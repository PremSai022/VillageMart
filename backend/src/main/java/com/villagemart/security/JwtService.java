package com.villagemart.security;

import com.villagemart.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final String ISSUER = "villagemart";

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(AppProperties properties) {
        String secret = properties.security().jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(properties.security().accessTokenTtlMinutes());
    }

    public String createAccessToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class), claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
