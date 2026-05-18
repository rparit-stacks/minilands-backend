package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.VoteChoice;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "property_votes")
@CompoundIndex(name = "proposal_investor_idx", def = "{'proposalId': 1, 'investorId': 1}", unique = true)
public class PropertyVote {

    @Id
    private String id;

    private String proposalId;
    private String investorId;
    private VoteChoice vote;
    private Instant votedAt;
    private Instant updatedAt;

    public PropertyVote() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProposalId() {
        return proposalId;
    }

    public void setProposalId(String proposalId) {
        this.proposalId = proposalId;
    }

    public String getInvestorId() {
        return investorId;
    }

    public void setInvestorId(String investorId) {
        this.investorId = investorId;
    }

    public VoteChoice getVote() {
        return vote;
    }

    public void setVote(VoteChoice vote) {
        this.vote = vote;
    }

    public Instant getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(Instant votedAt) {
        this.votedAt = votedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
