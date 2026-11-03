package com.villagemart.notification;

import com.villagemart.common.PageResponse;
import com.villagemart.notification.NotificationDtos.DeviceRegistrationRequest;
import com.villagemart.notification.NotificationDtos.NotificationDto;
import com.villagemart.notification.NotificationDtos.UnreadCountDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationDto> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.list(unreadOnly, page, size);
    }

    @GetMapping("/active-alerts")
    public List<NotificationDto> activeAlerts() {
        return service.activeAlerts();
    }

    @GetMapping("/unread-count")
    public UnreadCountDto unreadCount() {
        return new UnreadCountDto(service.unreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id) {
        return service.markRead(id);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", service.markAllRead());
    }

    @PostMapping("/devices")
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        service.registerDevice(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/devices/unregister")
    public ResponseEntity<Void> unregisterDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        service.unregisterDevice(request.token());
        return ResponseEntity.noContent().build();
    }
}
