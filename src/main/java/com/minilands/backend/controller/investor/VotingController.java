package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.voting.SaleVoteStatusResponse;
import com.minilands.backend.service.voting.PropertyVotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voting")
public class VotingController {

    private final PropertyVotingService propertyVotingService;

    public VotingController(PropertyVotingService propertyVotingService) {
        this.propertyVotingService = propertyVotingService;
    }

    @PostMapping("/properties/{propertyId}/opt-in")
    public ResponseEntity<SaleVoteStatusResponse> optIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String propertyId) {
        return ResponseEntity.ok(propertyVotingService.optIn(principal.getUserId(), propertyId));
    }

    @PostMapping("/properties/{propertyId}/opt-out")
    public ResponseEntity<SaleVoteStatusResponse> optOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String propertyId) {
        return ResponseEntity.ok(propertyVotingService.optOut(principal.getUserId(), propertyId));
    }

    @GetMapping("/properties/{propertyId}/status")
    public ResponseEntity<SaleVoteStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String propertyId) {
        return ResponseEntity.ok(propertyVotingService.getStatus(principal.getUserId(), propertyId));
    }
}
