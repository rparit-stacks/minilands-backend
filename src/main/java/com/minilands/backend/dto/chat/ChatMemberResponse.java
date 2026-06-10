package com.minilands.backend.dto.chat;

/**
 * A chat group member (for the admin moderation view).
 */
public record ChatMemberResponse(
        String userId,
        String name,
        String email,
        boolean muted,
        boolean removed
) {
}
