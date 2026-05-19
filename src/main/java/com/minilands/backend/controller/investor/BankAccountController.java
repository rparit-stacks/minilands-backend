package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.wallet.BankAccountRequest;
import com.minilands.backend.dto.wallet.BankAccountResponse;
import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.service.wallet.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> addAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.addAccount(principal.getUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> listAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bankAccountService.listAccounts(principal.getUserId()));
    }

    @PutMapping("/{bankAccountId}/primary")
    public ResponseEntity<BankAccountResponse> setPrimary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bankAccountId) {
        return ResponseEntity.ok(bankAccountService.setPrimary(principal.getUserId(), bankAccountId));
    }

    @DeleteMapping("/{bankAccountId}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bankAccountId) {
        bankAccountService.deleteAccount(principal.getUserId(), bankAccountId);
        return ResponseEntity.noContent().build();
    }
}
