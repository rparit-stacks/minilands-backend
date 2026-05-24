package com.minilands.backend.service.auth;

import com.minilands.backend.config.OtpProperties;
import com.minilands.backend.service.email.EmailSender;
import com.minilands.backend.service.email.EmailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends branded HTML one-time-passcode emails. Renders via {@link EmailTemplateService}
 * and dispatches via {@link EmailSender}. Falls back to a dev log if mail isn't configured.
 */
@Service
public class OtpEmailService {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailService.class);

    private final EmailTemplateService templates;
    private final EmailSender sender;
    private final OtpProperties otpProperties;

    public OtpEmailService(
            EmailTemplateService templates,
            EmailSender sender,
            OtpProperties otpProperties) {
        this.templates = templates;
        this.sender = sender;
        this.otpProperties = otpProperties;
    }

    public void sendOtp(String email, String otp) {
        int validMinutes = otpProperties.getExpirationMinutes() > 0 ? otpProperties.getExpirationMinutes() : 10;
        EmailTemplateService.Rendered rendered = templates.renderOtp(otp, validMinutes);
        if (!sender.isConfigured()) {
            log.warn("Mail not configured. OTP for {} is {} (dev only)", email, otp);
            return;
        }
        sender.send(email, rendered);
    }
}
