package com.minilands.backend.service.notification;

import com.minilands.backend.dto.notification.BroadcastNotificationRequest;
import com.minilands.backend.dto.notification.NotificationResponse;
import com.minilands.backend.dto.notification.RegisterPushDeviceRequest;
import com.minilands.backend.entity.enums.NotificationType;

import java.util.List;
import java.util.Map;

/**
 * Application facade: in-app inbox + outbound delivery (email, mobile push).
 */
public interface NotificationService {

    List<NotificationResponse> getNotifications(String userId);

    List<NotificationResponse> getUnread(String userId);

    void markAsRead(String userId, String notificationId);

    void send(String userId, NotificationType type, String title, String message);

    /// Rich variant used by the admin panel: lets the caller attach an image,
    /// a deep-link, and arbitrary metadata that the client forwards on tap.
    void sendRich(
            String userId,
            NotificationType type,
            String title,
            String message,
            String imageUrl,
            String deepLink,
            Map<String, String> data);

    /// Fan-out to many users at once based on a target audience.
    /// Returns the number of recipients actually targeted (after filtering).
    int broadcast(BroadcastNotificationRequest request);

    void registerPushDevice(String userId, RegisterPushDeviceRequest request);
}
