package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.user.UpdateProfileRequest;
import com.minilands.backend.dto.user.UserProfileResponse;
import com.minilands.backend.service.user.UserProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[ProfileController] GET /api/profile — userId={}", principal.getUserId());
        UserProfileResponse response = userProfileService.getProfile(principal.getUserId());
        log.info("[ProfileController] getProfile ok — name={} phone={} onboardingCompleted={} kycStatus={}",
                response.name(), response.phone(), response.onboardingCompleted(), response.kycStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("[ProfileController] PATCH /api/profile — userId={} name={} phone={} hasPhoto={}",
                principal.getUserId(), request.name(), request.phone(), request.profilePictureUrl() != null);
        UserProfileResponse response = userProfileService.updateProfile(principal.getUserId(), request);
        log.info("[ProfileController] updateProfile ok — onboardingCompleted={} kycStatus={}",
                response.onboardingCompleted(), response.kycStatus());
        return ResponseEntity.ok(response);
    }
}
