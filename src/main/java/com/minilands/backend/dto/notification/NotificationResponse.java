package com.minilands.backend.dto.notification;

import com.minilands.backend.entity.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String id,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
}
