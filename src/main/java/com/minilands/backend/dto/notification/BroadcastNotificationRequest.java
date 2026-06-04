package com.minilands.backend.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.minilands.backend.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Admin broadcast — sends a single notification to multiple recipients.
 *
 * <p>Targeting modes:
 * <ul>
 *   <li>{@code ALL_USERS} — every registered investor.</li>
 *   <li>{@code KYC_APPROVED} — only KYC-verified investors.</li>
 *   <li>{@code KYC_PENDING} — only investors with pending / rejected KYC.</li>
 *   <li>{@code INVESTORS} — users who hold at least one active position.</li>
 *   <li>{@code USER_IDS} — explicit list (`userIds` required).</li>
 * </ul>
 *
 * <p>Rich payload fields:
 * <ul>
 *   <li>{@code imageUrl} — optional banner image rendered above the body in the system tray.</li>
 *   <li>{@code deepLink} — opaque app route the client should open on tap.
 *       Format: {@code /screen/sub?key=val} (e.g. {@code /property/abc123},
 *       {@code /wallet}, {@code /holding/xyz}).</li>
 *   <li>{@code data} — extra key/value pairs forwarded as-is to the client.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BroadcastNotificationRequest(
        @NotNull TargetType targetType,
        List<String> userIds, // required when targetType == USER_IDS
        @NotNull NotificationType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String deepLink,
        Map<String, String> data
) {
    public enum TargetType {
        ALL_USERS,
        KYC_APPROVED,
        KYC_PENDING,
        INVESTORS,
        USER_IDS
    }
}
