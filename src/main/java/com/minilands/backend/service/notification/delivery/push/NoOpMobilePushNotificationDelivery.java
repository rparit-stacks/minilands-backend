package com.minilands.backend.service.notification.delivery.push;

import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.notification.delivery.MobilePushNotificationDelivery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default push delivery when no real provider (e.g. OneSignal) is wired.
 */
@Service
@ConditionalOnProperty(name = "app.notification.push-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMobilePushNotificationDelivery implements MobilePushNotificationDelivery {

    @Override
    public void deliver(NotificationMessage message) {
        // Push provider not configured — in-app (+ email) still delivered.
    }
}
