package com.villagemart.notification;

import com.villagemart.common.ApiException;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.notification.NotificationDtos.DeviceRegistrationRequest;
import com.villagemart.notification.NotificationDtos.NotificationDto;
import com.villagemart.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(boolean unreadOnly, int page, int size) {
        return PageResponse.of(repository.search(unreadOnly,
                Pages.of(page, size, null, Set.of(), Sort.by(Sort.Direction.DESC, "createdAt", "id"))), NotificationService::toDto);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> activeAlerts() {
        return repository.findActiveAlerts().stream().map(NotificationService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByReadFalse();
    }

    @Transactional
    public NotificationDto markRead(Long id) {
        Notification n = repository.findById(id).orElseThrow(() -> ApiException.notFound("Notification", id));
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
        }
        return toDto(n);
    }

    @Transactional
    public int markAllRead() {
        return repository.markAllRead(Instant.now());
    }

    @Transactional
    public void registerDevice(DeviceRegistrationRequest r) {
        Long userId = CurrentUser.require().id();
        DeviceToken token = deviceTokenRepository.findByToken(r.token().trim()).orElseGet(DeviceToken::new);
        token.setToken(r.token().trim());
        token.setUserId(userId);
        token.setPlatform(r.platform() == null || r.platform().isBlank() ? "ANDROID" : r.platform().trim().toUpperCase());
        token.setActive(true);
        deviceTokenRepository.save(token);
    }

    @Transactional
    public void unregisterDevice(String token) {
        deviceTokenRepository.findByToken(token.trim()).ifPresent(d -> d.setActive(false));
    }

    static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getSeverity(), n.getTitle(), n.getMessage(),
                n.getProduct() == null ? null : n.getProduct().getId(),
                n.getProduct() == null ? null : n.getProduct().getName(),
                n.isRead(), n.getReadAt(), n.isAlertActive(), n.getResolvedAt(), n.getPushStatus(), n.getCreatedAt());
    }
}
