package com.minilands.backend.dto.kyc;

import com.minilands.backend.entity.enums.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitKycDocumentRequest(
        @NotNull KycDocumentType documentType,
        @NotBlank String documentUrl
) {
}
