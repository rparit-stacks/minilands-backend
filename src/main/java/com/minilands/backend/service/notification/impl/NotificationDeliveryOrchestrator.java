package com.minilands.backend.service.notification.impl;

import com.minilands.backend.config.NotificationProperties;
import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.notification.delivery.EmailNotificationDelivery;
import com.minilands.backend.service.notification.delivery.MobilePushNotificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationDeliveryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryOrchestrator.class);

    private final NotificationProperties properties;
    private final EmailNotificationDelivery emailDelivery;
    private final MobilePushNotificationDelivery mobilePushDelivery;

    public NotificationDeliveryOrchestrator(
            NotificationProperties properties,
            EmailNotificationDelivery emailDelivery,
            MobilePushNotificationDelivery mobilePushDelivery) {
        this.properties = properties;
        this.emailDelivery = emailDelivery;
        this.mobilePushDelivery = mobilePushDelivery;
    }

    public void deliverOutbound(NotificationMessage message) {
        if (properties.isEmailEnabled() && StringUtils.hasText(message.recipientEmail())) {
            safeDeliver(() -> emailDelivery.deliver(message), "email", message.userId());
        }

        if (properties.isPushEnabled() && StringUtils.hasText(message.mobilePushPlayerId())) {
            safeDeliver(() -> mobilePushDelivery.deliver(message), "push", message.userId());
        }
    }

    private void safeDeliver(Runnable action, String channel, String userId) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("Failed {} delivery for user {}: {}", channel, userId, ex.getMessage());
        }
    }
}
