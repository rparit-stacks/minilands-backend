package com.minilands.backend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankAccountRequest(
        @NotBlank @Size(max = 100) String accountHolderName,
        @NotBlank @Size(min = 9, max = 18) String accountNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code") String ifscCode,
        @NotBlank @Size(max = 100) String bankName,
        boolean primary
) {
}
