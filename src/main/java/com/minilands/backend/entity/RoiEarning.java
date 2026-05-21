package com.minilands.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "roi_earnings")
public class RoiEarning {

    @Id
    private String id;

    @Indexed
    private String roiDistributionId;

    @Indexed(sparse = true)
    private String monthlyPaymentRunId;

    @Indexed
    private String holdingId;

    @Indexed
    private String userId;

    @Indexed
    private String propertyId;

    private BigDecimal amount;
    private BigDecimal roiPercentage;
    private Instant earnedOnDate;
    private Instant createdAt;

    public RoiEarning() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoiDistributionId() {
        return roiDistributionId;
    }

    public void setRoiDistributionId(String roiDistributionId) {
        this.roiDistributionId = roiDistributionId;
    }

    public String getMonthlyPaymentRunId() {
        return monthlyPaymentRunId;
    }

    public void setMonthlyPaymentRunId(String monthlyPaymentRunId) {
        this.monthlyPaymentRunId = monthlyPaymentRunId;
    }

    public String getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(String holdingId) {
        this.holdingId = holdingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRoiPercentage() {
        return roiPercentage;
    }

    public void setRoiPercentage(BigDecimal roiPercentage) {
        this.roiPercentage = roiPercentage;
    }

    public Instant getEarnedOnDate() {
        return earnedOnDate;
    }

    public void setEarnedOnDate(Instant earnedOnDate) {
        this.earnedOnDate = earnedOnDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
