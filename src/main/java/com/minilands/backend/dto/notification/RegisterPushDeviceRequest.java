package com.minilands.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;

/**
 * OneSignal subscription / player id from the mobile app after SDK init.
 */
public record RegisterPushDeviceRequest(
        @NotBlank String playerId
) {
}
