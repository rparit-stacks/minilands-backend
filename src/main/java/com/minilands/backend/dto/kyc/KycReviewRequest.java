package com.minilands.backend.dto.kyc;

import com.minilands.backend.entity.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record KycReviewRequest(
        @NotNull ApprovalStatus status,
        String note
) {
}
