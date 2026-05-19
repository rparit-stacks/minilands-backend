package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.wallet")
public class WalletProperties {

    private BigDecimal minDepositAmount = new BigDecimal("1");
    private BigDecimal maxDepositAmount = new BigDecimal("1000000");
    private BigDecimal minWithdrawalAmount = new BigDecimal("1");
    private BigDecimal maxWithdrawalAmount = new BigDecimal("1000000");

    public BigDecimal getMinDepositAmount() {
        return minDepositAmount;
    }

    public void setMinDepositAmount(BigDecimal minDepositAmount) {
        this.minDepositAmount = minDepositAmount;
    }

    public BigDecimal getMaxDepositAmount() {
        return maxDepositAmount;
    }

    public void setMaxDepositAmount(BigDecimal maxDepositAmount) {
        this.maxDepositAmount = maxDepositAmount;
    }

    public BigDecimal getMinWithdrawalAmount() {
        return minWithdrawalAmount;
    }

    public void setMinWithdrawalAmount(BigDecimal minWithdrawalAmount) {
        this.minWithdrawalAmount = minWithdrawalAmount;
    }

    public BigDecimal getMaxWithdrawalAmount() {
        return maxWithdrawalAmount;
    }

    public void setMaxWithdrawalAmount(BigDecimal maxWithdrawalAmount) {
        this.maxWithdrawalAmount = maxWithdrawalAmount;
    }
}
