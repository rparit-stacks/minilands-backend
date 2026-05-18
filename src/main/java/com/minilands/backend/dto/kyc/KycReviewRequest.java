package com.minilands.backend.dto.kyc;

import com.minilands.backend.entity.enums.ApprovalStatus;

public record KycReviewRequest(
        ApprovalStatus status,
        String note
) {
}
