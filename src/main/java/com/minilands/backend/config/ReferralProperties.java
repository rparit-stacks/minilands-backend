package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Static (env-level) referral knobs. The *rewards* themselves are admin-editable
 * at runtime via {@code ReferralSettings}; this only holds infra-level config
 * like the deep-link host and code shape.
 */
@ConfigurationProperties(prefix = "app.referral")
public class ReferralProperties {

    /** Base of the shareable referral link, e.g. https://openlink.minilands.in/refer. */
    private String linkBaseUrl = "https://openlink.minilands.in/refer";

    /** Length of the generated alphanumeric referral code. */
    private int codeLength = 7;

    public String getLinkBaseUrl() {
        return linkBaseUrl;
    }

    public void setLinkBaseUrl(String linkBaseUrl) {
        this.linkBaseUrl = linkBaseUrl;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }
}
