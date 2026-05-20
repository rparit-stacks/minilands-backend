package com.minilands.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One record = one investor opted in to sell this property.
 * No record = opted out. Unique per (propertyId, investorId).
 */
@Document(collection = "property_votes")
@CompoundIndex(name = "property_investor_idx", def = "{'propertyId': 1, 'investorId': 1}", unique = true)
public class PropertyVote {

    @Id
    private String id;

    private String propertyId;
    private String investorId;
    private Instant optedInAt;
    private Instant updatedAt;

    public PropertyVote() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }

    public Instant getOptedInAt() { return optedInAt; }
    public void setOptedInAt(Instant optedInAt) { this.optedInAt = optedInAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
