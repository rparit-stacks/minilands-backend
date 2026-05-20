package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.razorpay")
public class RazorpayProperties {

    private String keyId;
    private String keySecret;
    private String webhookSecret;
    private String companyName = "Minilands";

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    /**
     * Secret used to verify {@code X-Razorpay-Signature} on webhook POST bodies.
     * Must match the webhook signing secret from the Razorpay dashboard — not the API key secret.
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();
    }
}
