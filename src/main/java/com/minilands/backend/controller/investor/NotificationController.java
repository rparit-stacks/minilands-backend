package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.notification.NotificationResponse;
import com.minilands.backend.dto.notification.RegisterPushDeviceRequest;
import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.service.notification.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getNotifications(principal.getUserId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnread(principal.getUserId()));
    }

    @PutMapping("/push-device")
    public ResponseEntity<Void> registerPushDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterPushDeviceRequest request) {
        notificationService.registerPushDevice(principal.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String notificationId) {
        notificationService.markAsRead(principal.getUserId(), notificationId);
        return ResponseEntity.ok().build();
    }
}
