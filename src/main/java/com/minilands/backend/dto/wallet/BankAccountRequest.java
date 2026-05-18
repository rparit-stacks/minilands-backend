package com.minilands.backend.dto.wallet;

public record BankAccountRequest(
        String accountHolderName,
        String accountNumber,
        String ifscCode,
        String bankName,
        boolean primary
) {
}
