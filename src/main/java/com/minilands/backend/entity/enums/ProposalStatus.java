package com.minilands.backend.entity.enums;

public enum ProposalStatus {
    ACTIVE,
    PENDING_ADMIN_APPROVAL,
    APPROVED,
    /** Admin has recorded sale proceeds and the bulk pro-rata distribution has run. */
    DISTRIBUTED,
    REJECTED,
    EXPIRED
}
