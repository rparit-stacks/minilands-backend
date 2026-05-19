package com.minilands.backend.dto.notification;

import com.minilands.backend.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin-only: manually trigger a notification to a specific user.
 */
public record SendNotificationRequest(
        @NotBlank String userId,
        @NotNull NotificationType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String message
) {
}
