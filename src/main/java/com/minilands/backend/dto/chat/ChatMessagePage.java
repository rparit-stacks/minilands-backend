package com.minilands.backend.dto.chat;

import java.util.List;

/** Paginated chat history (newest-first pages). */
public record ChatMessagePage(
        List<ChatMessageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
