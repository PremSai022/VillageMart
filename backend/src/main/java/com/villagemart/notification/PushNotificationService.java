package com.villagemart.notification;

import com.villagemart.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * Delivers persisted notifications through FCM after the stock transaction commits, with scheduled retries.
 * Delivery failures never affect business transactions.
 */
@Slf4j
@Service
public class PushNotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseMessagingGateway gateway;
    private final TransactionTemplate tx;
    private final int maxAttempts;

    public PushNotificationService(NotificationRepository notificationRepository, DeviceTokenRepository deviceTokenRepository,
                                   FirebaseMessagingGateway gateway, PlatformTransactionManager txManager,
                                   AppProperties properties) {
        this.notificationRepository = notificationRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.gateway = gateway;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        int configured = properties.firebase() == null ? 0 : properties.firebase().maxPushAttempts();
        this.maxAttempts = configured > 0 ? configured : 5;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        deliver(event.notificationId());
    }

    @Scheduled(fixedDelayString = "${villagemart.firebase.push-retry-interval-ms:300000}",
            initialDelayString = "${villagemart.firebase.push-retry-interval-ms:300000}")
    public void retryFailed() {
        List<Long> ids = tx.execute(s -> notificationRepository
                .findTop50ByPushStatusAndPushAttemptsLessThanOrderByIdAsc(PushStatus.FAILED, maxAttempts)
                .stream().map(Notification::getId).toList());
        if (ids != null) {
            ids.forEach(this::deliver);
        }
    }

    public void deliver(Long notificationId) {
        try {
            tx.executeWithoutResult(s -> deliverInTx(notificationId));
        } catch (RuntimeException e) {
            log.warn("Push delivery for notification {} failed: {}", notificationId, e.getMessage());
        }
    }

    private void deliverInTx(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId).orElse(null);
        if (n == null || n.getPushStatus() == PushStatus.SENT || n.getPushStatus() == PushStatus.SKIPPED) {
            return;
        }
        if (!gateway.isEnabled()) {
            n.setPushStatus(PushStatus.SKIPPED);
            n.setLastPushError("Firebase not configured");
            return;
        }
        List<DeviceToken> devices = deviceTokenRepository.findByActiveTrue();
        if (devices.isEmpty()) {
            n.setPushStatus(PushStatus.SKIPPED);
            n.setLastPushError("No registered devices");
            return;
        }
        n.setPushAttempts(n.getPushAttempts() + 1);
        Map<String, String> data = Map.of(
                "notificationId", String.valueOf(n.getId()),
                "type", n.getType().name(),
                "productId", n.getProduct() == null ? "" : String.valueOf(n.getProduct().getId()));
        FirebaseMessagingGateway.SendResult result = gateway.send(
                devices.stream().map(DeviceToken::getToken).toList(), n.getTitle(), n.getMessage(), data);
        if (!result.invalidTokens().isEmpty()) {
            deviceTokenRepository.findByTokenIn(result.invalidTokens()).forEach(d -> d.setActive(false));
        }
        if (result.successCount() > 0) {
            n.setPushStatus(PushStatus.SENT);
            n.setLastPushError(null);
        } else {
            n.setPushStatus(PushStatus.FAILED);
            String err = result.error() == null ? "All device tokens rejected" : result.error();
            n.setLastPushError(err.length() > 300 ? err.substring(0, 300) : err);
        }
    }
}
