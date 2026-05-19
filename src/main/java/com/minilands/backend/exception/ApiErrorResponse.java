package com.minilands.backend.exception;

import java.time.Instant;

/**
 * Standard API error body for security and global handlers.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
