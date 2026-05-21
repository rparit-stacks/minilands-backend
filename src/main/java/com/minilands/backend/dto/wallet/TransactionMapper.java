package com.minilands.backend.dto.wallet;

import com.minilands.backend.entity.Transaction;

/** Shared mapping for API transaction DTOs (investor + admin). */
public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getReferenceId());
    }
}
