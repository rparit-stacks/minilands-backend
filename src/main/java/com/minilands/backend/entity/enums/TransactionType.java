package com.minilands.backend.entity.enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    INVESTMENT,
    SALE,
    ROI,
    /** Individual exit (legacy/per-investor flow). */
    EXIT,
    /** Pro-rata distribution from a bulk property-sale proceeds run. */
    EXIT_PROCEEDS,
    /** Manual credit by admin (ledger + transaction row). */
    ADMIN_CREDIT,
    /** Manual debit by admin (ledger + transaction row). */
    ADMIN_DEBIT
}
