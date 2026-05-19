package com.minilands.backend.dto.notification;

import com.minilands.backend.entity.enums.NotificationType;

import java.util.Map;

/**
 * Outbound notification payload — in-app record + optional email / push delivery.
 */
public record NotificationMessage(
        String userId,
        String recipientEmail,
        String mobilePushPlayerId,
        NotificationType type,
        String title,
        String body,
        Map<String, String> metadata
) {
    public NotificationMessage(
            String userId,
            String recipientEmail,
            String mobilePushPlayerId,
            NotificationType type,
            String title,
            String body) {
        this(userId, recipientEmail, mobilePushPlayerId, type, title, body, Map.of());
    }
}
