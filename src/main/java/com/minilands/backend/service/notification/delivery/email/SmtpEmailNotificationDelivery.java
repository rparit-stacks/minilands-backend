package com.minilands.backend.service.notification.delivery.email;

import com.minilands.backend.config.NotificationProperties;
import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.notification.delivery.EmailNotificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(name = "app.notification.email-enabled", havingValue = "true")
public class SmtpEmailNotificationDelivery implements EmailNotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailNotificationDelivery.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final NotificationProperties notificationProperties;

    public SmtpEmailNotificationDelivery(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailFrom,
            NotificationProperties notificationProperties) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public void deliver(NotificationMessage message) {
        if (mailSender == null || !StringUtils.hasText(mailFrom)) {
            log.warn("SMTP not configured; skipped email to user {}", message.userId());
            return;
        }
        if (!StringUtils.hasText(message.recipientEmail())) {
            log.warn("No email for user {}; skipped email delivery", message.userId());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(formatFrom());
        mail.setTo(message.recipientEmail());
        mail.setSubject(message.title());
        mail.setText(message.body());
        mailSender.send(mail);
    }

    private String formatFrom() {
        String name = notificationProperties.getEmailFromName();
        if (StringUtils.hasText(name)) {
            return name + " <" + mailFrom + ">";
        }
        return mailFrom;
    }
}
