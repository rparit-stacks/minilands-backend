package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.ProposalStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "property_sale_proposals")
public class PropertySaleProposal {

    @Id
    private String id;

    @Indexed
    private String propertyId;

    private String initiatedBy;
    private ProposalStatus status;
    private Instant votingStartDate;
    private Instant votingEndDate;
    private Integer totalVotesNeeded;
    private Integer votesReceived;
    private Integer votesYes;
    private Integer votesNo;
    private Instant createdAt;

    public PropertySaleProposal() {
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

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Instant getVotingStartDate() {
        return votingStartDate;
    }

    public void setVotingStartDate(Instant votingStartDate) {
        this.votingStartDate = votingStartDate;
    }

    public Instant getVotingEndDate() {
        return votingEndDate;
    }

    public void setVotingEndDate(Instant votingEndDate) {
        this.votingEndDate = votingEndDate;
    }

    public Integer getTotalVotesNeeded() {
        return totalVotesNeeded;
    }

    public void setTotalVotesNeeded(Integer totalVotesNeeded) {
        this.totalVotesNeeded = totalVotesNeeded;
    }

    public Integer getVotesReceived() {
        return votesReceived;
    }

    public void setVotesReceived(Integer votesReceived) {
        this.votesReceived = votesReceived;
    }

    public Integer getVotesYes() {
        return votesYes;
    }

    public void setVotesYes(Integer votesYes) {
        this.votesYes = votesYes;
    }

    public Integer getVotesNo() {
        return votesNo;
    }

    public void setVotesNo(Integer votesNo) {
        this.votesNo = votesNo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
