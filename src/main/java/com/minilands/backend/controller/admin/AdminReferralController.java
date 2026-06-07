package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.referral.ReferralSettingsDto;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.referral.ReferralSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin management of the referral programme: reward tiers, friend bonus,
 * and the on/off switch.
 */
@RestController
@RequestMapping("/api/admin/referral-settings")
public class AdminReferralController {

    private final ReferralSettingsService settingsService;

    public AdminReferralController(ReferralSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<ReferralSettingsDto> get(
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(settingsService.getDto());
    }

    @PutMapping
    public ResponseEntity<ReferralSettingsDto> update(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody ReferralSettingsDto request) {
        return ResponseEntity.ok(settingsService.update(request));
    }
}
