package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.ProposalStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One record per property — tracks the current sale-vote state.
 * Opt-in counts are derived live from PropertyVote records (propertyId field).
 */
@Document(collection = "property_sale_proposals")
public class PropertySaleProposal {

    @Id
    private String id;

    @Indexed(unique = true)
    private String propertyId;

    private ProposalStatus status;
    private Instant thresholdReachedAt;

    private String reviewedByAdminId;
    private String adminNote;
    private Instant reviewedAt;
    private Instant createdAt;

    public PropertySaleProposal() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }

    public Instant getThresholdReachedAt() { return thresholdReachedAt; }
    public void setThresholdReachedAt(Instant thresholdReachedAt) { this.thresholdReachedAt = thresholdReachedAt; }

    public String getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(String reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
