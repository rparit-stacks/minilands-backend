package com.minilands.backend.dto.chat;

import com.minilands.backend.entity.enums.ChatMessageType;

import java.time.Instant;

/**
 * A chat message as sent to clients (app + admin) and broadcast over STOMP.
 */
public record ChatMessageResponse(
        String id,
        String propertyId,
        String senderId,
        String senderName,
        boolean fromAdmin,
        ChatMessageType type,
        String text,
        String mediaUrl,
        String mediaName,
        String mediaMimeType,
        boolean deleted,
        Instant createdAt
) {
}
