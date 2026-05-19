package com.minilands.backend.service.kyc.impl;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.entity.KycDocument;
import com.minilands.backend.entity.enums.KycDocumentType;

import java.util.EnumSet;
import java.util.Set;

final class KycSupport {

    static final Set<KycDocumentType> REQUIRED_DOCUMENT_TYPES =
            EnumSet.allOf(KycDocumentType.class);

    private KycSupport() {
    }

    static KycDocumentResponse toResponse(KycDocument document) {
        return new KycDocumentResponse(
                document.getId(),
                document.getUserId(),
                document.getDocumentType(),
                document.getDocumentUrl(),
                document.getStatus(),
                document.getReviewedByAdminId(),
                document.getReviewNote(),
                document.getReviewedAt(),
                document.getCreatedAt());
    }
}
