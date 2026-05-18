package com.minilands.backend.dto.kyc;

import com.minilands.backend.entity.enums.KycDocumentType;

public record SubmitKycDocumentRequest(
        KycDocumentType documentType,
        String documentUrl
) {
}
