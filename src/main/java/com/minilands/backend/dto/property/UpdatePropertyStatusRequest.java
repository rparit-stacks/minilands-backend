package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyStatusRequest(
        @NotNull PropertyStatus status
) {
}
