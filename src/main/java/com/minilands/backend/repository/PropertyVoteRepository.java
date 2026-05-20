package com.minilands.backend.repository;

import com.minilands.backend.entity.PropertyVote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyVoteRepository extends MongoRepository<PropertyVote, String> {

    List<PropertyVote> findByPropertyId(String propertyId);

    Optional<PropertyVote> findByPropertyIdAndInvestorId(String propertyId, String investorId);

    long countByPropertyId(String propertyId);

    void deleteByPropertyIdAndInvestorId(String propertyId, String investorId);
}
