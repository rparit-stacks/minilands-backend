package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.admin.AdminResponse;
import com.minilands.backend.dto.admin.CreateAdminRequest;
import com.minilands.backend.dto.admin.InviteAdminRequest;
import com.minilands.backend.dto.admin.InviteAdminResponse;
import com.minilands.backend.dto.admin.UpdateAdminRequest;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.admin.AdminManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/admins")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @GetMapping
    public ResponseEntity<List<AdminResponse>> list(
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(adminManagementService.listAdmins());
    }

    @GetMapping("/{adminId}")
    public ResponseEntity<AdminResponse> get(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String adminId) {
        return ResponseEntity.ok(adminManagementService.getAdmin(adminId));
    }

    @PostMapping
    public ResponseEntity<AdminResponse> create(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminManagementService.createAdmin(request));
    }

    @PostMapping("/invite")
    public ResponseEntity<InviteAdminResponse> invite(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody InviteAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminManagementService.inviteAdmin(principal.getAdminId(), request));
    }

    @PatchMapping("/{adminId}")
    public ResponseEntity<AdminResponse> update(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String adminId,
            @Valid @RequestBody UpdateAdminRequest request) {
        return ResponseEntity.ok(adminManagementService.updateAdmin(adminId, request));
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String adminId) {
        adminManagementService.deleteAdmin(principal.getAdminId(), adminId);
        return ResponseEntity.noContent().build();
    }
}
