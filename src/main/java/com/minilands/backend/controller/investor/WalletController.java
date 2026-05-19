package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.payment.DepositResponse;
import com.minilands.backend.dto.payment.InitiateDepositRequest;
import com.minilands.backend.dto.payment.InitiateDepositResponse;
import com.minilands.backend.dto.payment.ReportDepositRequest;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.dto.wallet.WalletBalanceResponse;
import com.minilands.backend.dto.wallet.WithdrawalRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.service.wallet.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getBalance(principal.getUserId()));
    }

    /** Step 1–2: create deposit order + Razorpay payment order (frontend opens Checkout next). */
    @PostMapping("/deposits/initiate")
    public ResponseEntity<InitiateDepositResponse> initiateDeposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitiateDepositRequest request) {
        return ResponseEntity.ok(walletService.initiateDeposit(principal.getUserId(), request));
    }

    /** Optional: frontend reports outcome if webhook is delayed (same commit/rollback rules). */
    @PostMapping("/deposits/report")
    public ResponseEntity<DepositResponse> reportDeposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportDepositRequest request) {
        return ResponseEntity.ok(walletService.reportDeposit(principal.getUserId(), request));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<WithdrawalResponse> requestWithdrawal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(walletService.requestWithdrawal(principal.getUserId(), request));
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<List<WithdrawalResponse>> getWithdrawals(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getWithdrawals(principal.getUserId()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getTransactionHistory(principal.getUserId()));
    }
}
