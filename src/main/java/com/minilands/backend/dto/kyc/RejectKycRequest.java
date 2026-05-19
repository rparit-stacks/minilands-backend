package com.minilands.backend.dto.kyc;

import jakarta.validation.constraints.NotBlank;

public record RejectKycRequest(
        @NotBlank String note
) {
}
