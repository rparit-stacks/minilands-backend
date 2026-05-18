package com.minilands.backend.repository;

import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.enums.HoldingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyHoldingRepository extends MongoRepository<PropertyHolding, String> {

    List<PropertyHolding> findByUserId(String userId);

    List<PropertyHolding> findByPropertyId(String propertyId);

    Optional<PropertyHolding> findByUserIdAndPropertyId(String userId, String propertyId);

    List<PropertyHolding> findByUserIdAndStatus(String userId, HoldingStatus status);

    List<PropertyHolding> findByPropertyIdAndStatus(String propertyId, HoldingStatus status);
}
