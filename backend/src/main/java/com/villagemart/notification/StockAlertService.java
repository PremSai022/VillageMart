package com.villagemart.notification;

import com.villagemart.inventory.StockStatus;
import com.villagemart.product.Product;
import com.villagemart.settings.BusinessSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Transition-based stock alerts. Called by InventoryService only when a product's stock status changes, so repeated
 * requests never produce duplicate notifications for the same low-stock episode.
 * <ul>
 *   <li>NORMAL -> LOW_STOCK: new LOW_STOCK alert.</li>
 *   <li>any -> OUT_OF_STOCK: resolves active alerts and creates an OUT_OF_STOCK alert.</li>
 *   <li>OUT_OF_STOCK -> LOW_STOCK: resolves the out-of-stock alert; same episode, no new notification.</li>
 *   <li>any -> NORMAL: resolves active alerts so the next drop starts a new episode.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StockAlertService {

    private final NotificationRepository repository;
    private final BusinessSettingsService settingsService;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.MANDATORY)
    public void onStatusTransition(Product product, StockStatus previous, StockStatus current, BigDecimal quantity) {
        if (previous == current) {
            return;
        }
        Instant now = Instant.now();
        List<Notification> active = repository.findByProductIdAndAlertActiveTrue(product.getId());
        switch (current) {
            case NORMAL -> resolve(active, now);
            case OUT_OF_STOCK -> {
                resolve(active, now);
                create(product, NotificationType.OUT_OF_STOCK, NotificationSeverity.CRITICAL,
                        "Out of stock: " + product.getName(),
                        "%s is out of stock. Minimum stock is %s %s. Restock soon.".formatted(product.getName(),
                                plain(product.getMinStockQuantity()), product.getUnit().getCode()));
            }
            case LOW_STOCK -> {
                if (previous == StockStatus.OUT_OF_STOCK) {
                    resolve(active, now);
                } else if (active.isEmpty()) {
                    create(product, NotificationType.LOW_STOCK, NotificationSeverity.WARNING,
                            "Low stock: " + product.getName(),
                            "Only %s %s of %s left (minimum %s).".formatted(plain(quantity), product.getUnit().getCode(),
                                    product.getName(), plain(product.getMinStockQuantity())));
                }
            }
        }
    }

    private void resolve(List<Notification> active, Instant now) {
        for (Notification n : active) {
            n.setAlertActive(false);
            n.setResolvedAt(now);
        }
    }

    private void create(Product product, NotificationType type, NotificationSeverity severity, String title, String message) {
        Notification n = new Notification();
        n.setType(type);
        n.setSeverity(severity);
        n.setTitle(title);
        n.setMessage(message);
        n.setProduct(product);
        n.setAlertActive(true);
        n.setPushStatus(settingsService.getEntity().isLowStockPushEnabled() ? PushStatus.PENDING : PushStatus.SKIPPED);
        repository.save(n);
        if (n.getPushStatus() == PushStatus.PENDING) {
            events.publishEvent(new NotificationCreatedEvent(n.getId()));
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
