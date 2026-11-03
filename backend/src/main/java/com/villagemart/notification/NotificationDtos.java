package com.villagemart.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationDto(Long id, NotificationType type, NotificationSeverity severity, String title,
                                  String message, Long productId, String productName, boolean read, Instant readAt,
                                  boolean alertActive, Instant resolvedAt, PushStatus pushStatus, Instant createdAt) {
    }

    public record UnreadCountDto(long count) {
    }

    public record DeviceRegistrationRequest(
            @NotBlank @Size(max = 512) String token,
            @Size(max = 20) String platform) {
    }
}
