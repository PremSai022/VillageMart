package com.villagemart.notification;

import com.villagemart.common.BaseEntity;
import com.villagemart.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "alert_active", nullable = false)
    private boolean alertActive;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false)
    private PushStatus pushStatus;

    @Column(name = "push_attempts", nullable = false)
    private int pushAttempts;

    @Column(name = "last_push_error")
    private String lastPushError;
}
