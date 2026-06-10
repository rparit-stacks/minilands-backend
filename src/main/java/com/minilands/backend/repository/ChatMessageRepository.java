package com.minilands.backend.repository;

import com.minilands.backend.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** Paginated history, newest first, hiding soft-deleted messages (investor view). */
    Page<ChatMessage> findByPropertyIdAndDeletedFalseOrderByCreatedAtDesc(String propertyId, Pageable pageable);

    /** Paginated history including deleted (admin moderation view). */
    Page<ChatMessage> findByPropertyIdOrderByCreatedAtDesc(String propertyId, Pageable pageable);
}
