package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Branding constants used by transactional emails (and any other branded surface).
 * Lets us swap product name, URL, support address, etc. via env without touching templates.
 */
@ConfigurationProperties(prefix = "app.brand")
public class BrandProperties {

    private String name = "Minilands";
    private String tagline = "Fractional real-estate investing";
    private String websiteUrl = "https://minilands.in";
    private String supportEmail = "help.minilands@gmail.com";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }
}
