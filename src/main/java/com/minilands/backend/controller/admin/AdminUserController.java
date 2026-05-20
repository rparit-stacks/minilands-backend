package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.admin.AdminUserResponse;
import com.minilands.backend.dto.admin.UpdateUserRequest;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.admin.AdminUserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserManagementService userManagementService;

    public AdminUserController(AdminUserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /** GET /api/admin/users?accountStatus=ACTIVE&kycStatus=PENDING */
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) AccountStatus accountStatus,
            @RequestParam(required = false) KycStatus kycStatus) {
        return ResponseEntity.ok(userManagementService.listUsers(accountStatus, kycStatus));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> get(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId) {
        return ResponseEntity.ok(userManagementService.getUser(userId));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> update(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userManagementService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
