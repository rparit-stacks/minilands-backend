package com.minilands.backend.dto.admin;

import com.minilands.backend.dto.wallet.TransactionResponse;

import java.util.List;

/** Paginated ledger view for a user wallet (admin). */
public record AdminWalletTransactionsPage(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
