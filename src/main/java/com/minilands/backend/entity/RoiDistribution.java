package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.RoiDistributionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "roi_distributions")
@CompoundIndex(
        name = "property_period_unique",
        def = "{'propertyId': 1, 'distributionYear': 1, 'distributionMonth': 1}",
        unique = true
)
public class RoiDistribution {

    @Id
    private String id;

    @Indexed
    private String propertyId;

    private Integer distributionYear;
    private Integer distributionMonth;
    private BigDecimal roiPercentage;
    private BigDecimal totalDistributed;
    private RoiDistributionStatus status;
    private Instant distributedAt;
    private Instant createdAt;

    public RoiDistribution() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public Integer getDistributionYear() {
        return distributionYear;
    }

    public void setDistributionYear(Integer distributionYear) {
        this.distributionYear = distributionYear;
    }

    public Integer getDistributionMonth() {
        return distributionMonth;
    }

    public void setDistributionMonth(Integer distributionMonth) {
        this.distributionMonth = distributionMonth;
    }

    public BigDecimal getRoiPercentage() {
        return roiPercentage;
    }

    public void setRoiPercentage(BigDecimal roiPercentage) {
        this.roiPercentage = roiPercentage;
    }

    public BigDecimal getTotalDistributed() {
        return totalDistributed;
    }

    public void setTotalDistributed(BigDecimal totalDistributed) {
        this.totalDistributed = totalDistributed;
    }

    public RoiDistributionStatus getStatus() {
        return status;
    }

    public void setStatus(RoiDistributionStatus status) {
        this.status = status;
    }

    public Instant getDistributedAt() {
        return distributedAt;
    }

    public void setDistributedAt(Instant distributedAt) {
        this.distributedAt = distributedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
