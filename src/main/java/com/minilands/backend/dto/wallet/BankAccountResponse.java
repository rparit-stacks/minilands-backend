package com.minilands.backend.dto.wallet;

public record BankAccountResponse(
        String id,
        String accountHolderName,
        String accountNumber,
        String ifscCode,
        String bankName,
        boolean primary,
        boolean verified
) {
}
