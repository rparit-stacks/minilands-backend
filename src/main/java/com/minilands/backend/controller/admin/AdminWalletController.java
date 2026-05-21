package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.admin.AdminWalletAdjustRequest;
import com.minilands.backend.dto.admin.AdminWalletDetailResponse;
import com.minilands.backend.dto.admin.AdminWalletRowResponse;
import com.minilands.backend.dto.admin.AdminWalletTransactionsPage;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.admin.AdminWalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wallets")
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    public AdminWalletController(AdminWalletService adminWalletService) {
        this.adminWalletService = adminWalletService;
    }

    /** GET /api/admin/wallets — all investor wallets with user email/name. */
    @GetMapping
    public ResponseEntity<List<AdminWalletRowResponse>> list(@AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(adminWalletService.listAll());
    }

    /** GET /api/admin/wallets/{userId} — balance, available, pending withdrawals, recent withdrawal list. */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminWalletDetailResponse> detail(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId) {
        return ResponseEntity.ok(adminWalletService.getDetail(userId));
    }

    /**
     * GET /api/admin/wallets/{userId}/transactions — paginated ledger (newest first).
     * Optional filter: type=DEPOSIT, ADMIN_CREDIT, etc.
     */
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<AdminWalletTransactionsPage> transactions(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminWalletService.listTransactions(userId, type, page, size));
    }

    /**
     * POST /api/admin/wallets/{userId}/adjust — credit or debit wallet (ledger + transaction).
     * Body: { "amount": 100.50, "direction": "CREDIT" | "DEBIT", "note": "optional" }
     */
    @PostMapping("/{userId}/adjust")
    public ResponseEntity<AdminWalletRowResponse> adjust(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody AdminWalletAdjustRequest request) {
        return ResponseEntity.ok(adminWalletService.adjust(userId, request, principal.getAdminId()));
    }
}
