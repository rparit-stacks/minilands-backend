package com.minilands.backend.dto.chat;

import com.minilands.backend.entity.enums.ChatMessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for sending a chat message (used by both the REST fallback and the
 * STOMP {@code @MessageMapping} handler). Media is uploaded separately via the
 * media endpoint; the resulting URL is passed here.
 */
public record SendChatMessageRequest(
        @NotNull ChatMessageType type,
        @Size(max = 4000) String text,
        @Size(max = 1000) String mediaUrl,
        @Size(max = 300) String mediaName,
        @Size(max = 150) String mediaMimeType
) {
}
