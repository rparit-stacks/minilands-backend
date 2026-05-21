package com.minilands.backend.service.admin;

import com.minilands.backend.dto.admin.AdminWalletAdjustRequest;
import com.minilands.backend.dto.admin.AdminWalletDetailResponse;
import com.minilands.backend.dto.admin.AdminWalletRowResponse;
import com.minilands.backend.dto.admin.AdminWalletTransactionsPage;
import com.minilands.backend.entity.enums.TransactionType;

import java.util.List;

public interface AdminWalletService {

    List<AdminWalletRowResponse> listAll();

    AdminWalletRowResponse adjust(String userId, AdminWalletAdjustRequest request, String actingAdminId);

    AdminWalletDetailResponse getDetail(String userId);

    AdminWalletTransactionsPage listTransactions(String userId, TransactionType type, int page, int size);
}
