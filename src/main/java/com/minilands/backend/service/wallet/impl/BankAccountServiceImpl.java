package com.minilands.backend.service.wallet.impl;

import com.minilands.backend.dto.wallet.BankAccountRequest;
import com.minilands.backend.dto.wallet.BankAccountResponse;
import com.minilands.backend.entity.BankAccount;
import com.minilands.backend.entity.enums.WithdrawalStatus;
import com.minilands.backend.repository.BankAccountRepository;
import com.minilands.backend.repository.WithdrawalRepository;
import com.minilands.backend.service.wallet.BankAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final WithdrawalRepository withdrawalRepository;

    public BankAccountServiceImpl(
            BankAccountRepository bankAccountRepository,
            WithdrawalRepository withdrawalRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.withdrawalRepository = withdrawalRepository;
    }

    @Override
    @Transactional
    public BankAccountResponse addAccount(String userId, BankAccountRequest request) {
        List<BankAccount> existing = bankAccountRepository.findByUserId(userId);
        boolean makePrimary = request.primary() || existing.isEmpty();

        if (makePrimary) {
            WalletSupport.clearPrimaryFlags(existing);
        }

        Instant now = Instant.now();
        BankAccount account = new BankAccount();
        account.setUserId(userId);
        account.setAccountHolderName(request.accountHolderName().trim());
        account.setAccountNumber(request.accountNumber().trim());
        account.setIfscCode(request.ifscCode().trim().toUpperCase());
        account.setBankName(request.bankName().trim());
        account.setPrimary(makePrimary);
        account.setVerified(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        bankAccountRepository.save(account);

        if (makePrimary && !existing.isEmpty()) {
            bankAccountRepository.saveAll(existing);
        }

        return WalletSupport.toBankAccountResponse(account);
    }

    @Override
    public List<BankAccountResponse> listAccounts(String userId) {
        return bankAccountRepository.findByUserId(userId).stream()
                .map(WalletSupport::toBankAccountResponse)
                .toList();
    }

    @Override
    @Transactional
    public BankAccountResponse setPrimary(String userId, String bankAccountId) {
        BankAccount account = requireOwnedAccount(userId, bankAccountId);
        List<BankAccount> accounts = bankAccountRepository.findByUserId(userId);
        WalletSupport.clearPrimaryFlags(accounts);
        account.setPrimary(true);
        account.setUpdatedAt(Instant.now());
        bankAccountRepository.saveAll(accounts);
        bankAccountRepository.save(account);
        return WalletSupport.toBankAccountResponse(account);
    }

    @Override
    @Transactional
    public void deleteAccount(String userId, String bankAccountId) {
        BankAccount account = requireOwnedAccount(userId, bankAccountId);

        boolean hasPendingWithdrawal = withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(w -> w.getStatus() == WithdrawalStatus.PENDING
                        && bankAccountId.equals(w.getBankAccountId()));

        if (hasPendingWithdrawal) {
            throw new IllegalArgumentException("Cannot delete bank account with a pending withdrawal");
        }

        bankAccountRepository.delete(account);

        if (account.isPrimary()) {
            List<BankAccount> remaining = bankAccountRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                BankAccount next = remaining.getFirst();
                next.setPrimary(true);
                next.setUpdatedAt(Instant.now());
                bankAccountRepository.save(next);
            }
        }
    }

    private BankAccount requireOwnedAccount(String userId, String bankAccountId) {
        return bankAccountRepository.findById(bankAccountId)
                .filter(account -> account.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));
    }
}
