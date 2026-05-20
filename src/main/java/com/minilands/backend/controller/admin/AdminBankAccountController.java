package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.wallet.BankAccountResponse;
import com.minilands.backend.entity.BankAccount;
import com.minilands.backend.repository.BankAccountRepository;
import com.minilands.backend.security.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bank-accounts")
public class AdminBankAccountController {

    private final BankAccountRepository bankAccountRepository;

    public AdminBankAccountController(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    /** GET /api/admin/bank-accounts?userId=xxx  — list all accounts for a user */
    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> listByUser(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam String userId) {
        List<BankAccountResponse> accounts = bankAccountRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{bankAccountId}")
    public ResponseEntity<BankAccountResponse> getById(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String bankAccountId) {
        BankAccount account = findAccount(bankAccountId);
        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/{bankAccountId}/verify")
    public ResponseEntity<BankAccountResponse> verify(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String bankAccountId) {
        BankAccount account = findAccount(bankAccountId);
        if (account.isVerified()) {
            throw new IllegalStateException("Bank account is already verified");
        }
        account.setVerified(true);
        account.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toResponse(bankAccountRepository.save(account)));
    }

    @PostMapping("/{bankAccountId}/unverify")
    public ResponseEntity<BankAccountResponse> unverify(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String bankAccountId) {
        BankAccount account = findAccount(bankAccountId);
        account.setVerified(false);
        account.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toResponse(bankAccountRepository.save(account)));
    }

    private BankAccount findAccount(String bankAccountId) {
        return bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getAccountHolderName(),
                maskAccountNumber(account.getAccountNumber()),
                account.getIfscCode(),
                account.getBankName(),
                account.isPrimary(),
                account.isVerified());
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return accountNumber;
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
