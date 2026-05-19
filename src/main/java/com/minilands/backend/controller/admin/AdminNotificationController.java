package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.auth.MessageResponse;
import com.minilands.backend.dto.notification.SendNotificationRequest;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.notification.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only manual notification dispatch (support / ops). Investors cannot call this.
 */
@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AdminNotificationController(
            NotificationService notificationService,
            UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendToUser(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody SendNotificationRequest request) {
        userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        notificationService.send(
                request.userId(),
                request.type(),
                request.title(),
                request.message());

        return ResponseEntity.ok(new MessageResponse("Notification sent to user"));
    }
}
