package com.minilands.backend.service.admin;

import com.minilands.backend.service.email.EmailSender;
import com.minilands.backend.service.email.EmailTemplateService;
import org.springframework.stereotype.Service;

@Service
public class AdminInviteEmailService {

    private final EmailTemplateService templates;
    private final EmailSender sender;

    public AdminInviteEmailService(EmailTemplateService templates, EmailSender sender) {
        this.templates = templates;
        this.sender = sender;
    }

    /**
     * @param toEmail   the invitee's email
     * @param role      role name (e.g. "ADMIN", "SUPER_ADMIN")
     * @param setupUrl  full setup URL with one-time token
     * @param validHours how long the invite is valid (used in the body copy)
     * @return true if the email was actually sent; false if mail isn't configured or send failed
     */
    public boolean sendInvite(String toEmail, String role, String setupUrl, long validHours) {
        EmailTemplateService.Rendered rendered = templates.renderAdminInvite(toEmail, role, setupUrl, validHours);
        return sender.send(toEmail, rendered);
    }
}
