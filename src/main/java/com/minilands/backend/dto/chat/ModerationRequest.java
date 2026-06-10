package com.minilands.backend.dto.chat;

/**
 * Toggle a moderation flag (mute / remove) for a user in a property chat.
 */
public record ModerationRequest(
        boolean value
) {
}
