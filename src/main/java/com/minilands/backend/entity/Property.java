package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.PropertyStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "properties")
public class Property {

    @Id
    private String id;

    private String name;
    private String description;
    private String location;
    private BigDecimal totalTarget;
    private Integer totalShares;
    private BigDecimal sharePrice;
    private BigDecimal currentPrice;
    private BigDecimal annualRoi;
    private BigDecimal monthlyRoi;
    private PropertyStatus status;
    private Integer currentInvestors;
    private BigDecimal totalRaised;
    private Instant createdAt;
    private Instant updatedAt;

    public Property() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getTotalTarget() {
        return totalTarget;
    }

    public void setTotalTarget(BigDecimal totalTarget) {
        this.totalTarget = totalTarget;
    }

    public Integer getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(Integer totalShares) {
        this.totalShares = totalShares;
    }

    public BigDecimal getSharePrice() {
        return sharePrice;
    }

    public void setSharePrice(BigDecimal sharePrice) {
        this.sharePrice = sharePrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getAnnualRoi() {
        return annualRoi;
    }

    public void setAnnualRoi(BigDecimal annualRoi) {
        this.annualRoi = annualRoi;
    }

    public BigDecimal getMonthlyRoi() {
        return monthlyRoi;
    }

    public void setMonthlyRoi(BigDecimal monthlyRoi) {
        this.monthlyRoi = monthlyRoi;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public Integer getCurrentInvestors() {
        return currentInvestors;
    }

    public void setCurrentInvestors(Integer currentInvestors) {
        this.currentInvestors = currentInvestors;
    }

    public BigDecimal getTotalRaised() {
        return totalRaised;
    }

    public void setTotalRaised(BigDecimal totalRaised) {
        this.totalRaised = totalRaised;
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
