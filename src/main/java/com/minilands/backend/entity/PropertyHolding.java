package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.HoldingStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "property_holdings")
@CompoundIndex(name = "user_property_idx", def = "{'userId': 1, 'propertyId': 1}")
public class PropertyHolding {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String propertyId;

    private BigDecimal sharesOwned;
    private BigDecimal investmentAmount;
    private BigDecimal currentValue;
    private BigDecimal roiEarned;
    private BigDecimal costBasis;
    private HoldingStatus status;
    private Instant entryDate;
    private Instant withdrawnAt;
    private Instant createdAt;
    private Instant updatedAt;

    public PropertyHolding() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public BigDecimal getSharesOwned() {
        return sharesOwned;
    }

    public void setSharesOwned(BigDecimal sharesOwned) {
        this.sharesOwned = sharesOwned;
    }

    public BigDecimal getInvestmentAmount() {
        return investmentAmount;
    }

    public void setInvestmentAmount(BigDecimal investmentAmount) {
        this.investmentAmount = investmentAmount;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getRoiEarned() {
        return roiEarned;
    }

    public void setRoiEarned(BigDecimal roiEarned) {
        this.roiEarned = roiEarned;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public HoldingStatus getStatus() {
        return status;
    }

    public void setStatus(HoldingStatus status) {
        this.status = status;
    }

    public Instant getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(Instant entryDate) {
        this.entryDate = entryDate;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
