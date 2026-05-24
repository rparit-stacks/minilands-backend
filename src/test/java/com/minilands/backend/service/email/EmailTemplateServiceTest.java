package com.minilands.backend.service.email;

import com.minilands.backend.config.BrandProperties;
import com.minilands.backend.entity.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateServiceTest {

    private static final Pattern UNRESOLVED = Pattern.compile("%[A-Z][A-Z_]+%");

    private EmailTemplateService service() {
        BrandProperties brand = new BrandProperties();
        return new EmailTemplateService(brand);
    }

    private void assertWellFormed(EmailTemplateService.Rendered r, String mustContain) {
        assertNotNull(r);
        assertNotNull(r.subject());
        assertNotNull(r.html());
        assertNotNull(r.text());
        // Every %FOO% placeholder must be substituted before send.
        assertFalse(UNRESOLVED.matcher(r.html()).find(),
                "Unresolved placeholder found in HTML for subject '" + r.subject() + "'");
        assertTrue(r.html().contains(mustContain),
                "HTML missing expected content '" + mustContain + "'");
        assertTrue(r.html().contains("Minilands"), "HTML missing brand name");
    }

    @Test
    void otpTemplate_rendersOtpCode_andHasNoUnresolvedTokens() {
        EmailTemplateService.Rendered r = service().renderOtp("482917", 10);
        assertWellFormed(r, "482917");
        assertTrue(r.text().contains("482917"));
        assertTrue(r.subject().toLowerCase().contains("login code"));
    }

    @Test
    void inviteTemplate_includesSetupUrl_andRole() {
        String url = "https://admin.example.com/admin-setup?token=abc123";
        EmailTemplateService.Rendered r = service().renderAdminInvite("new@example.com", "ADMIN", url, 72);
        assertWellFormed(r, url);
        assertTrue(r.html().contains("ADMIN"));
        assertTrue(r.html().contains("72 hours"));
    }

    @Test
    void notificationTemplate_perType_pickAccentAndRenderBody() {
        EmailTemplateService svc = service();
        assertAll(
                () -> assertWellFormed(svc.renderNotification(NotificationType.DEPOSIT, "Deposit received", "₹500 added to your wallet."), "Deposit received"),
                () -> assertWellFormed(svc.renderNotification(NotificationType.KYC, "KYC approved", "You can now invest."), "KYC approved"),
                () -> assertWellFormed(svc.renderNotification(NotificationType.ROI, "Rent payout", "₹250 credited."), "Rent payout"),
                () -> assertWellFormed(svc.renderNotification(NotificationType.WITHDRAWAL, "Withdrawal queued", "We'll process within 24h."), "Withdrawal queued"),
                () -> assertWellFormed(svc.renderNotification(NotificationType.GENERAL, "Welcome", "Glad to have you on board."), "Welcome")
        );
    }

    @Test
    void notificationTemplate_handlesNullBody_safely() {
        EmailTemplateService.Rendered r = service().renderNotification(NotificationType.GENERAL, "Hello", null);
        assertNotNull(r.html());
        assertFalse(UNRESOLVED.matcher(r.html()).find());
    }
}
