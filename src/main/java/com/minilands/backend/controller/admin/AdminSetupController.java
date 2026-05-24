package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.admin.AdminResponse;
import com.minilands.backend.dto.admin.AdminSetupRequest;
import com.minilands.backend.dto.admin.InviteInfoResponse;
import com.minilands.backend.service.admin.AdminManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints used by the admin-setup page (no auth — token is the credential).
 */
@RestController
@RequestMapping("/api/admin/auth/setup")
public class AdminSetupController {

    private final AdminManagementService adminManagementService;

    public AdminSetupController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @GetMapping("/info")
    public ResponseEntity<InviteInfoResponse> info(@RequestParam("token") String token) {
        return ResponseEntity.ok(adminManagementService.getInviteInfo(token));
    }

    @PostMapping
    public ResponseEntity<AdminResponse> setup(@Valid @RequestBody AdminSetupRequest request) {
        return ResponseEntity.ok(adminManagementService.completeSetup(request));
    }
}
