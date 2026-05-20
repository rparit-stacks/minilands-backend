package com.minilands.backend.service.wallet;

import com.minilands.backend.dto.wallet.AdminActionRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.entity.enums.WithdrawalStatus;

import java.util.List;

public interface AdminWithdrawalService {

    List<WithdrawalResponse> listWithdrawals(WithdrawalStatus status);

    WithdrawalResponse getById(String withdrawalId);

    void approve(String adminId, String withdrawalId, AdminActionRequest request);

    void reject(String adminId, String withdrawalId, AdminActionRequest request);
}
