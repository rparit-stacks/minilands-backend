package com.minilands.backend.service.wallet;

import com.minilands.backend.dto.wallet.AdminActionRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;

import java.util.List;

/**
 * Admin approval of withdrawal requests (ISP — admin-only surface).
 */
public interface AdminWithdrawalService {

    List<WithdrawalResponse> listPendingWithdrawals();

    void approve(String adminId, String withdrawalId, AdminActionRequest request);

    void reject(String adminId, String withdrawalId, AdminActionRequest request);
}
