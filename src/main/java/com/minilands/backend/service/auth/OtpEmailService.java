package com.minilands.backend.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OtpEmailService {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public OtpEmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    public void sendOtp(String email, String otp) {
        if (mailSender == null || !StringUtils.hasText(mailFrom)) {
            log.warn("Mail not configured. OTP for {} is {} (dev only)", email, otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Your Minilands login code");
        message.setText("Your one-time login code is: " + otp + "\n\nValid for 10 minutes. Do not share this code.");
        mailSender.send(message);
    }
}
