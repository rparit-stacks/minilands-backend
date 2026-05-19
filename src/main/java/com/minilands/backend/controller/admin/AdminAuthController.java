package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.auth.AuthResponse;
import com.minilands.backend.dto.auth.LoginRequest;
import com.minilands.backend.dto.auth.MessageResponse;
import com.minilands.backend.dto.auth.RefreshTokenRequest;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.auth.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(adminAuthService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal AdminPrincipal principal) {
        adminAuthService.logout(principal.getAdminId());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
