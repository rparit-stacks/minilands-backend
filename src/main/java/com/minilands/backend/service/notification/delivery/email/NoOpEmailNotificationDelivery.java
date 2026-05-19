package com.minilands.backend.service.notification.delivery.email;

import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.notification.delivery.EmailNotificationDelivery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(EmailNotificationDelivery.class)
public class NoOpEmailNotificationDelivery implements EmailNotificationDelivery {

    @Override
    public void deliver(NotificationMessage message) {
        // Email channel disabled via app.notification.email-enabled=false
    }
}
