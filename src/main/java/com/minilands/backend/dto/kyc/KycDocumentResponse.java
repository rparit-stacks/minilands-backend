package com.minilands.backend.dto.kyc;

import com.minilands.backend.entity.enums.ApprovalStatus;
import com.minilands.backend.entity.enums.KycDocumentType;

import java.time.Instant;

public record KycDocumentResponse(
        String id,
        String userId,
        KycDocumentType documentType,
        String documentUrl,
        ApprovalStatus status,
        String reviewedByAdminId,
        String reviewNote,
        Instant reviewedAt,
        Instant createdAt
) {
}
