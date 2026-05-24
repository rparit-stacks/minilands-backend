package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable marketplace defaults. Per-property overrides live on the {@code Property} entity
 * (e.g. {@code marketplaceFeePercent}); these are platform-wide fallbacks.
 */
@ConfigurationProperties(prefix = "app.marketplace")
public class MarketplaceProperties {

    /** Days after creation when an ACTIVE listing auto-expires if not sold or cancelled. */
    private int listingExpiryDays = 60;

    public int getListingExpiryDays() {
        return listingExpiryDays;
    }

    public void setListingExpiryDays(int listingExpiryDays) {
        this.listingExpiryDays = listingExpiryDays;
    }
}
