package com.minilands.backend.service.email;

import com.minilands.backend.config.NotificationProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Sends a {@link EmailTemplateService.Rendered} email as a multipart MIME message (HTML body
 * with plain-text alternative). Falls back to dev-log if mail isn't configured.
 */
@Service
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;
    private final String mailFrom;

    public EmailSender(
            @Autowired(required = false) JavaMailSender mailSender,
            NotificationProperties notificationProperties,
            @Value("${spring.mail.username:}") String mailFrom) {
        this.mailSender = mailSender;
        this.notificationProperties = notificationProperties;
        this.mailFrom = mailFrom;
    }

    /** @return true if the email was actually dispatched; false if mail isn't configured or send failed. */
    public boolean send(String to, EmailTemplateService.Rendered rendered) {
        if (!isConfigured()) {
            log.warn("Mail not configured — skipping email to {}: {}", to, rendered.subject());
            return false;
        }
        if (!StringUtils.hasText(to)) {
            log.warn("No recipient email — skipping email: {}", rendered.subject());
            return false;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(formatFrom());
            helper.setTo(to);
            helper.setSubject(rendered.subject());
            // Order matters: setText(plain, html) sets plain-text fallback first, HTML as alternative.
            helper.setText(rendered.text() == null ? "" : rendered.text(),
                    rendered.html() == null ? "" : rendered.html());
            mailSender.send(mime);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email to {} ({}): {}", to, rendered.subject(), ex.getMessage());
            return false;
        }
    }

    public boolean isConfigured() {
        return mailSender != null && StringUtils.hasText(mailFrom);
    }

    private String formatFrom() {
        String name = notificationProperties.getEmailFromName();
        if (StringUtils.hasText(name)) {
            return name + " <" + mailFrom + ">";
        }
        return mailFrom;
    }
}
