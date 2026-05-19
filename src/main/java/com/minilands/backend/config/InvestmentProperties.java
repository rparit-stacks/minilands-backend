package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.investment")
public class InvestmentProperties {

    /** Platform fee on rent distributions (e.g. 10 = 10%). */
    private int rentPlatformFeePercent = 10;

    public int getRentPlatformFeePercent() {
        return rentPlatformFeePercent;
    }

    public void setRentPlatformFeePercent(int rentPlatformFeePercent) {
        this.rentPlatformFeePercent = rentPlatformFeePercent;
    }
}
