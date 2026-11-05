package com.villagemart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "villagemart")
public record AppProperties(
        String timezone,
        String currency,
        Security security,
        Cors cors,
        Owner owner,
        Business business,
        Firebase firebase) {

    public record Security(String jwtSecret, long accessTokenTtlMinutes, long refreshTokenTtlDays) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record Owner(String username, String mobile, String name, String password, String resetPassword) {
    }

    public record Business(String name, String address, String mobile) {
    }

    public record Firebase(String credentialsBase64, long pushRetryIntervalMs, int maxPushAttempts) {
    }
}
