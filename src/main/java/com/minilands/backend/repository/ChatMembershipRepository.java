package com.minilands.backend.repository;

import com.minilands.backend.entity.ChatMembership;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMembershipRepository extends MongoRepository<ChatMembership, String> {

    Optional<ChatMembership> findByPropertyIdAndUserId(String propertyId, String userId);

    /** All moderation overrides for a property (muted / removed users). */
    List<ChatMembership> findByPropertyId(String propertyId);
}
