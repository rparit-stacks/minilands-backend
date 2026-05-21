package com.minilands.backend.dto.admin;

import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.dto.wallet.WalletBalanceResponse;
import com.minilands.backend.dto.wallet.WithdrawalResponse;

import java.util.List;

/** Admin wallet drill-down: profile row + live balance + recent withdrawal requests. */
public record AdminWalletDetailResponse(
        AdminWalletRowResponse profile,
        WalletBalanceResponse balance,
        List<WithdrawalResponse> recentWithdrawals
) {
}
