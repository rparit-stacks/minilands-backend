package com.minilands.backend.service.email;

import com.minilands.backend.config.BrandProperties;
import com.minilands.backend.entity.enums.NotificationType;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * Renders branded HTML email bodies (plus a plain-text fallback). Templates are inline strings
 * so we don't need Thymeleaf or a templating engine for the small surface area we have.
 *
 * <p>The layout is a single &lt;table&gt;-based shell (max compatible with Outlook / Gmail clipping)
 * with the Minilands wordmark + inline SVG mark in the header, a colored accent strip, the content,
 * and a footer with support contact + brand tagline.
 */
@Service
public class EmailTemplateService {

    /* ── Brand colors (mirror admin dashboard theme: Hosting/primary) ─────────── */
    private static final String COLOR_PRIMARY = "#606BDF";
    private static final String COLOR_PRIMARY_DARK = "#3944B8";
    private static final String COLOR_PRIMARY_LIGHTER = "#E0E0FF";
    private static final String COLOR_TEXT = "#1B1B1F";
    private static final String COLOR_TEXT_SECONDARY = "#46464F";
    private static final String COLOR_TEXT_MUTED = "#777680";
    private static final String COLOR_SURFACE = "#FFFFFF";
    private static final String COLOR_SURFACE_ALT = "#F5F4FA";
    private static final String COLOR_BORDER = "#EFEDF4";

    /* Per-category accents — used as the header strip + icon tint for notifications. */
    private static final String COLOR_SUCCESS = "#2E7D32";
    private static final String COLOR_WARNING = "#ED6C02";
    private static final String COLOR_ERROR = "#D32F2F";
    private static final String COLOR_INFO = "#0288D1";

    private final BrandProperties brand;

    public EmailTemplateService(BrandProperties brand) {
        this.brand = brand;
    }

    /* ───────────────────────────────────────────────────────────────────────────
     * Public render methods
     * ─────────────────────────────────────────────────────────────────────────── */

    public Rendered renderOtp(String otp, int validMinutes) {
        String preheader = "Your one-time login code is " + otp + " — valid for " + validMinutes + " minutes.";
        String content = """
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:%TEXT%;">
                  Use the code below to finish signing in to your %BRAND% account.
                </p>
                <div style="margin:24px 0;padding:20px 24px;background:%PRIMARY_LIGHTER%;border-radius:14px;text-align:center;">
                  <div style="font-family:'SFMono-Regular',Consolas,Menlo,monospace;font-size:34px;letter-spacing:8px;font-weight:700;color:%PRIMARY_DARK%;">
                    %OTP%
                  </div>
                  <div style="margin-top:10px;font-size:12px;color:%PRIMARY_DARK%;letter-spacing:0.5px;text-transform:uppercase;font-weight:600;">
                    Valid for %MINUTES% minutes
                  </div>
                </div>
                <p style="margin:0;font-size:13px;line-height:1.6;color:%TEXT_SECONDARY%;">
                  If you didn't request this code, you can safely ignore this email — someone may have typed your address by mistake. Never share this code with anyone.
                </p>
                """
                .replace("%OTP%", escape(otp))
                .replace("%MINUTES%", Integer.toString(validMinutes))
                .replace("%BRAND%", escape(brand.getName()))
                .replace("%TEXT%", COLOR_TEXT)
                .replace("%TEXT_SECONDARY%", COLOR_TEXT_SECONDARY)
                .replace("%PRIMARY_LIGHTER%", COLOR_PRIMARY_LIGHTER)
                .replace("%PRIMARY_DARK%", COLOR_PRIMARY_DARK);

        String plain = """
                Use this one-time code to sign in to %s:

                %s

                Valid for %d minutes. If you didn't request this, ignore this email — never share this code.

                — %s
                """.formatted(brand.getName(), otp, validMinutes, brand.getName());

        String html = renderShell(
                "Your sign-in code",
                preheader,
                COLOR_PRIMARY,
                "Sign-in code",
                content);

        return new Rendered("Your " + brand.getName() + " login code", html, plain);
    }

    public Rendered renderAdminInvite(String inviteeEmail, String role, String setupUrl, long validHours) {
        String preheader = "You've been invited to join " + brand.getName() + " as " + role
                + ". Set up your account in the next " + validHours + " hours.";
        String content = """
                <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:%TEXT%;">
                  Hi there 👋
                </p>
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:%TEXT%;">
                  You've been invited to join the %BRAND% admin team as
                  <span style="display:inline-block;padding:2px 10px;background:%PRIMARY_LIGHTER%;color:%PRIMARY_DARK%;border-radius:999px;font-size:12px;font-weight:700;letter-spacing:0.3px;">%ROLE%</span>.
                </p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:%TEXT%;">
                  Click the button below to set your password and finish creating your account.
                </p>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 28px;">
                  <tr>
                    <td align="center">
                      <a href="%URL%" target="_blank"
                         style="display:inline-block;padding:14px 28px;background:%PRIMARY%;color:#FFFFFF;text-decoration:none;border-radius:10px;font-weight:700;font-size:15px;letter-spacing:0.2px;box-shadow:0 6px 14px -4px rgba(96,107,223,0.45);">
                        Set up your account →
                      </a>
                    </td>
                  </tr>
                </table>
                <div style="margin:0 0 16px;padding:14px 16px;background:%SURFACE_ALT%;border:1px solid %BORDER%;border-radius:10px;">
                  <div style="font-size:11px;color:%TEXT_MUTED%;font-weight:600;text-transform:uppercase;letter-spacing:0.4px;margin-bottom:6px;">Or copy this link</div>
                  <div style="font-family:'SFMono-Regular',Consolas,Menlo,monospace;font-size:12px;color:%TEXT_SECONDARY%;word-break:break-all;line-height:1.5;">%URL%</div>
                </div>
                <p style="margin:0;font-size:13px;line-height:1.6;color:%TEXT_SECONDARY%;">
                  This link is valid for <strong>%HOURS% hours</strong> and can be used <strong>once</strong>.
                  If you weren't expecting this invite, please ignore this email.
                </p>
                """
                .replace("%BRAND%", escape(brand.getName()))
                .replace("%ROLE%", escape(role))
                .replace("%URL%", setupUrl) // URL not escaped — must remain valid
                .replace("%HOURS%", Long.toString(validHours))
                .replace("%TEXT%", COLOR_TEXT)
                .replace("%TEXT_SECONDARY%", COLOR_TEXT_SECONDARY)
                .replace("%TEXT_MUTED%", COLOR_TEXT_MUTED)
                .replace("%PRIMARY%", COLOR_PRIMARY)
                .replace("%PRIMARY_DARK%", COLOR_PRIMARY_DARK)
                .replace("%PRIMARY_LIGHTER%", COLOR_PRIMARY_LIGHTER)
                .replace("%SURFACE_ALT%", COLOR_SURFACE_ALT)
                .replace("%BORDER%", COLOR_BORDER);

        String plain = """
                You've been invited to join %s as %s.

                Set up your account here (valid for %d hours, single use):
                %s

                If you weren't expecting this email, please ignore it.

                — %s
                """.formatted(brand.getName(), role, validHours, setupUrl, brand.getName());

        String html = renderShell(
                "You're invited to " + brand.getName(),
                preheader,
                COLOR_PRIMARY,
                "Admin invitation",
                content);

        return new Rendered("You're invited to join " + brand.getName() + " as an admin", html, plain);
    }

    /**
     * Generic notification — picks accent color from {@link NotificationType}. The {@code body}
     * passed in can be plain text; we wrap it in a styled paragraph but do not interpret HTML.
     */
    public Rendered renderNotification(NotificationType type, String title, String body) {
        Category cat = categoryFor(type);
        String preheader = title;
        String content = """
                <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:%TEXT%;white-space:pre-wrap;">%BODY%</p>
                """
                .replace("%BODY%", escape(body == null ? "" : body))
                .replace("%TEXT%", COLOR_TEXT);

        String html = renderShell(title, preheader, cat.color, cat.label, content);

        String plain = (title == null ? "" : title) + "\n\n"
                + (body == null ? "" : body) + "\n\n— " + brand.getName();

        return new Rendered(title, html, plain);
    }

    /* ───────────────────────────────────────────────────────────────────────────
     * Shell + helpers
     * ─────────────────────────────────────────────────────────────────────────── */

    /**
     * The shared shell — header (logo + wordmark), accent strip, content card, footer.
     * Uses table-based layout for Outlook compatibility. Inline styles only (no &lt;style&gt;
     * blocks since Gmail strips them in some cases).
     */
    private String renderShell(String subject, String preheader, String accentColor, String chipLabel, String content) {
        int year = Year.now().getValue();
        String websiteUrl = brand.getWebsiteUrl() == null ? "" : brand.getWebsiteUrl();
        return """
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <meta name="color-scheme" content="light only" />
                  <meta name="supported-color-schemes" content="light" />
                  <title>%SUBJECT%</title>
                </head>
                <body style="margin:0;padding:0;background:%SURFACE_ALT%;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:%TEXT%;">
                  <!-- Preheader (hidden but shown in inbox preview) -->
                  <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:%SURFACE_ALT%;">
                    %PREHEADER%
                  </div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%SURFACE_ALT%;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:560px;background:%SURFACE%;border:1px solid %BORDER%;border-radius:18px;overflow:hidden;">
                          <!-- Header -->
                          <tr>
                            <td style="padding:24px 28px 18px;border-bottom:1px solid %BORDER%;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                  <td style="vertical-align:middle;">
                                    <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                                      <td style="vertical-align:middle;padding-right:10px;">
                                        %LOGO%
                                      </td>
                                      <td style="vertical-align:middle;">
                                        <div style="font-size:18px;font-weight:800;letter-spacing:-0.4px;color:%TEXT%;line-height:1;">%BRAND%</div>
                                        <div style="font-size:11px;color:%TEXT_MUTED%;margin-top:3px;letter-spacing:0.2px;">%TAGLINE%</div>
                                      </td>
                                    </tr></table>
                                  </td>
                                  <td align="right" style="vertical-align:middle;">
                                    <span style="display:inline-block;padding:5px 11px;background:%ACCENT_BG%;color:%ACCENT%;border-radius:999px;font-size:11px;font-weight:700;letter-spacing:0.4px;text-transform:uppercase;">
                                      %CHIP%
                                    </span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <!-- Accent strip -->
                          <tr>
                            <td style="height:3px;background:%ACCENT%;line-height:3px;font-size:3px;">&nbsp;</td>
                          </tr>
                          <!-- Content -->
                          <tr>
                            <td style="padding:28px 28px 8px;">
                              %CONTENT%
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:24px 28px 26px;border-top:1px solid %BORDER%;background:%SURFACE_ALT%;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                  <td>
                                    <div style="font-size:12px;color:%TEXT_MUTED%;line-height:1.6;">
                                      Need help? Reply to this email or write to
                                      <a href="mailto:%SUPPORT%" style="color:%PRIMARY%;text-decoration:none;font-weight:600;">%SUPPORT%</a>.
                                    </div>
                                    <div style="font-size:11px;color:%TEXT_MUTED%;margin-top:10px;">
                                      © %YEAR% %BRAND% · <a href="%URL%" style="color:%TEXT_MUTED%;text-decoration:underline;">%URL_DISPLAY%</a>
                                    </div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                        <div style="font-size:11px;color:%TEXT_MUTED%;margin-top:14px;max-width:560px;line-height:1.6;">
                          You received this email because it relates to your %BRAND% account. We never ask for passwords or OTPs over email or phone.
                        </div>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .replace("%SUBJECT%", escape(subject))
                .replace("%PREHEADER%", escape(preheader))
                .replace("%CONTENT%", content)
                .replace("%LOGO%", logoSvg())
                .replace("%BRAND%", escape(brand.getName()))
                .replace("%TAGLINE%", escape(brand.getTagline()))
                .replace("%CHIP%", escape(chipLabel))
                .replace("%ACCENT%", accentColor)
                .replace("%ACCENT_BG%", tintFor(accentColor))
                .replace("%SUPPORT%", escape(brand.getSupportEmail()))
                .replace("%URL%", websiteUrl)
                .replace("%URL_DISPLAY%", escape(websiteUrl.replaceFirst("^https?://", "")))
                .replace("%YEAR%", Integer.toString(year))
                .replace("%PRIMARY%", COLOR_PRIMARY)
                .replace("%TEXT%", COLOR_TEXT)
                .replace("%TEXT_MUTED%", COLOR_TEXT_MUTED)
                .replace("%SURFACE%", COLOR_SURFACE)
                .replace("%SURFACE_ALT%", COLOR_SURFACE_ALT)
                .replace("%BORDER%", COLOR_BORDER);
    }

    /**
     * Inline SVG mark — the same stylized "M" used in the admin sidebar. Inlining (instead of
     * hosting a PNG) avoids broken images when clients block remote content and keeps the email
     * payload small.
     */
    private String logoSvg() {
        return """
                <svg width="32" height="32" viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg" style="display:block;">
                  <rect width="28" height="28" rx="7" fill="%PRIMARY%"/>
                  <path d="M6 21V8l8 6.5L22 8v13" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                </svg>
                """
                .replace("%PRIMARY%", COLOR_PRIMARY);
    }

    /** Per-NotificationType accent + short chip label shown in the header. */
    private Category categoryFor(NotificationType type) {
        if (type == null) {
            return new Category(COLOR_PRIMARY, "Update");
        }
        return switch (type) {
            case KYC        -> new Category(COLOR_INFO, "KYC update");
            case DEPOSIT    -> new Category(COLOR_SUCCESS, "Deposit");
            case WITHDRAWAL -> new Category(COLOR_WARNING, "Withdrawal");
            case INVESTMENT -> new Category(COLOR_PRIMARY, "Investment");
            case ROI        -> new Category(COLOR_SUCCESS, "Rent payout");
            case VOTE       -> new Category(COLOR_PRIMARY_DARK, "Vote");
            case EXIT       -> new Category(COLOR_ERROR, "Exit / Sale");
            case GENERAL    -> new Category(COLOR_PRIMARY, "Update");
        };
    }

    /**
     * Soft tinted background for the header chip. Maps each accent to a hand-picked light tint
     * for legible chip backgrounds across clients (we avoid alpha colors since some clients drop them).
     */
    private String tintFor(String accent) {
        return switch (accent) {
            case COLOR_SUCCESS -> "#E8F5E9";
            case COLOR_WARNING -> "#FFF4E5";
            case COLOR_ERROR   -> "#FDECEA";
            case COLOR_INFO    -> "#E3F2FD";
            case COLOR_PRIMARY_DARK,
                 COLOR_PRIMARY -> COLOR_PRIMARY_LIGHTER;
            default            -> COLOR_PRIMARY_LIGHTER;
        };
    }

    /** Minimal HTML escape for values interpolated into templates. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /* ───────────────────────────────────────────────────────────────────────────
     * Value types
     * ─────────────────────────────────────────────────────────────────────────── */

    /** Rendered email — subject + HTML body + plain-text fallback. */
    public record Rendered(String subject, String html, String text) {
    }

    private record Category(String color, String label) {
    }
}
