package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.auth.AuthResponse;
import com.minilands.backend.dto.auth.GoogleAuthRequest;
import com.minilands.backend.dto.auth.MessageResponse;
import com.minilands.backend.dto.auth.RefreshTokenRequest;
import com.minilands.backend.dto.auth.SendOtpRequest;
import com.minilands.backend.dto.auth.VerifyOtpRequest;
import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.service.auth.UserAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/otp/send")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        userAuthService.sendEmailOtp(request);
        return ResponseEntity.ok(new MessageResponse("OTP sent to your email"));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(userAuthService.verifyEmailOtp(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(userAuthService.authenticateWithGoogle(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userAuthService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal UserPrincipal principal) {
        userAuthService.logout(principal.getUserId());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
