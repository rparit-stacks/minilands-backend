package com.minilands.backend.dto.notification;

import com.minilands.backend.entity.enums.NotificationType;

import java.util.Map;

/**
 * Outbound notification payload — in-app record + optional email / push delivery.
 *
 * <p>Push-specific extras:
 * <ul>
 *   <li>{@code imageUrl} — optional banner image rendered in the notification tray.</li>
 *   <li>{@code deepLink} — opaque client route to open on tap (e.g. {@code /property/abc}).</li>
 * </ul>
 */
public record NotificationMessage(
        String userId,
        String recipientEmail,
        String mobilePushPlayerId,
        NotificationType type,
        String title,
        String body,
        String imageUrl,
        String deepLink,
        Map<String, String> metadata
) {
    /// Convenience constructor for the most common case — text-only, no image,
    /// no deep link, no metadata. Used by the existing service hooks
    /// (KYC approved, investment confirmed, ROI payout, etc.).
    public NotificationMessage(
            String userId,
            String recipientEmail,
            String mobilePushPlayerId,
            NotificationType type,
            String title,
            String body) {
        this(userId, recipientEmail, mobilePushPlayerId, type, title, body, null, null, Map.of());
    }
}
