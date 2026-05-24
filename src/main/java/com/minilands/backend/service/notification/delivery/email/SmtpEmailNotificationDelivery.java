package com.minilands.backend.service.notification.delivery.email;

import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.email.EmailSender;
import com.minilands.backend.service.email.EmailTemplateService;
import com.minilands.backend.service.notification.delivery.EmailNotificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Renders the notification title + body inside the branded shell (with an accent color picked
 * per {@link com.minilands.backend.entity.enums.NotificationType}) and dispatches over SMTP.
 */
@Service
@ConditionalOnProperty(name = "app.notification.email-enabled", havingValue = "true")
public class SmtpEmailNotificationDelivery implements EmailNotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailNotificationDelivery.class);

    private final EmailTemplateService templates;
    private final EmailSender sender;

    public SmtpEmailNotificationDelivery(EmailTemplateService templates, EmailSender sender) {
        this.templates = templates;
        this.sender = sender;
    }

    @Override
    public void deliver(NotificationMessage message) {
        if (!StringUtils.hasText(message.recipientEmail())) {
            log.warn("No email for user {}; skipped email delivery", message.userId());
            return;
        }
        EmailTemplateService.Rendered rendered = templates.renderNotification(
                message.type(), message.title(), message.body());
        sender.send(message.recipientEmail(), rendered);
    }
}
