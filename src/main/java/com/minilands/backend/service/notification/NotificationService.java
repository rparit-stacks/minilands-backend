package com.minilands.backend.service.notification;

import com.minilands.backend.dto.notification.NotificationResponse;
import com.minilands.backend.entity.enums.NotificationType;

import java.util.List;

/**
 * User notifications — read/mark; other services publish via this contract (DIP).
 */
public interface NotificationService {

    List<NotificationResponse> getNotifications(String userId);

    List<NotificationResponse> getUnread(String userId);

    void markAsRead(String userId, String notificationId);

    void send(String userId, NotificationType type, String title, String message);
}
