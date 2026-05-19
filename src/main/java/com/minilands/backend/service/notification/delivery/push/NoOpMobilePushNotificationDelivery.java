package com.minilands.backend.service.notification.delivery.push;

import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.notification.delivery.MobilePushNotificationDelivery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Placeholder until a OneSignal (or other) {@link MobilePushNotificationDelivery} is added.
 */
@Service
@ConditionalOnMissingBean(MobilePushNotificationDelivery.class)
public class NoOpMobilePushNotificationDelivery implements MobilePushNotificationDelivery {

    @Override
    public void deliver(NotificationMessage message) {
        // Push provider not configured — in-app (+ email) still delivered.
    }
}
