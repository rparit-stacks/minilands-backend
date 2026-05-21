package com.minilands.backend.entity.enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    INVESTMENT,
    SALE,
    ROI,
    EXIT,
    /** Manual credit by admin (ledger + transaction row). */
    ADMIN_CREDIT,
    /** Manual debit by admin (ledger + transaction row). */
    ADMIN_DEBIT
}
