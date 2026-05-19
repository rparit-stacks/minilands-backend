package com.minilands.backend.dto.kyc;

import com.minilands.backend.entity.enums.KycStatus;

import java.time.Instant;
import java.util.List;

public record KycStatusResponse(
        KycStatus kycStatus,
        Instant kycVerifiedAt,
        String kycRejectionNote,
        boolean allDocumentsSubmitted,
        List<KycDocumentResponse> documents
) {
}
