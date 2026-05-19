package com.minilands.backend.service.notification.delivery;

import com.minilands.backend.dto.notification.NotificationMessage;

/**
 * Delivers notifications to mobile devices (OneSignal or any push provider).
 * Implementation not wired yet — use {@link com.minilands.backend.service.notification.delivery.push.NoOpMobilePushNotificationDelivery}.
 */
public interface MobilePushNotificationDelivery {

    void deliver(NotificationMessage message);
}
