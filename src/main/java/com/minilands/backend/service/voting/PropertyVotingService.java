package com.minilands.backend.service.voting;

import com.minilands.backend.dto.voting.ProposalResponse;
import com.minilands.backend.dto.voting.VoteRequest;
import com.minilands.backend.dto.voting.VoteResponse;

/**
 * Property sale proposals and 70% vote mechanism (SRP).
 */
public interface PropertyVotingService {

    ProposalResponse initiateSale(String userId, String propertyId);

    VoteResponse castVote(String userId, String proposalId, VoteRequest request);

    ProposalResponse getProposal(String proposalId);

    void finalizeVoting(String proposalId);
}
