package com.villagemart.security;

public record AuthenticatedUser(Long id, String username, String role) {
}
