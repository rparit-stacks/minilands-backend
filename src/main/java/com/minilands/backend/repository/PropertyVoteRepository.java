package com.minilands.backend.repository;

import com.minilands.backend.entity.PropertyVote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyVoteRepository extends MongoRepository<PropertyVote, String> {

    List<PropertyVote> findByProposalId(String proposalId);

    Optional<PropertyVote> findByProposalIdAndInvestorId(String proposalId, String investorId);

    long countByProposalId(String proposalId);
}
