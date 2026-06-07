package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.referral.ReferralSummaryResponse;
import com.minilands.backend.dto.referral.ReferredUserResponse;
import com.minilands.backend.service.referral.ReferralService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Investor-facing "Refer & Earn" endpoints.
 */
@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    /// Everything the Refer & Earn screen needs: code, link, totals, tier reward.
    @GetMapping("/summary")
    public ResponseEntity<ReferralSummaryResponse> summary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(referralService.getSummary(principal.getUserId()));
    }

    /// The friends this user has referred, newest first.
    @GetMapping("/users")
    public ResponseEntity<List<ReferredUserResponse>> referredUsers(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(referralService.getReferredUsers(principal.getUserId()));
    }
}
