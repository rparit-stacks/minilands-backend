package com.minilands.backend.service.notification.delivery;

import com.minilands.backend.dto.notification.NotificationMessage;

/**
 * Delivers notifications over email (SMTP).
 */
public interface EmailNotificationDelivery {

    void deliver(NotificationMessage message);
}
