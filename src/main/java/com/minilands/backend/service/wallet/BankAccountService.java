package com.minilands.backend.service.wallet;

import com.minilands.backend.dto.wallet.BankAccountRequest;
import com.minilands.backend.dto.wallet.BankAccountResponse;

import java.util.List;

/**
 * Bank account management — segregated from {@link WalletService} (ISP).
 */
public interface BankAccountService {

    BankAccountResponse addAccount(String userId, BankAccountRequest request);

    List<BankAccountResponse> listAccounts(String userId);

    BankAccountResponse setPrimary(String userId, String bankAccountId);

    void deleteAccount(String userId, String bankAccountId);
}
