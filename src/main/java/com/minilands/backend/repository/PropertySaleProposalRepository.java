package com.minilands.backend.repository;

import com.minilands.backend.entity.PropertySaleProposal;
import com.minilands.backend.entity.enums.ProposalStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PropertySaleProposalRepository extends MongoRepository<PropertySaleProposal, String> {

    List<PropertySaleProposal> findByPropertyId(String propertyId);

    List<PropertySaleProposal> findByStatus(ProposalStatus status);

    Optional<PropertySaleProposal> findByPropertyIdAndStatus(String propertyId, ProposalStatus status);
}
