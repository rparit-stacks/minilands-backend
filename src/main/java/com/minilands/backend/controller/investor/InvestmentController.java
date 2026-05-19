package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.investment.BuySharesRequest;
import com.minilands.backend.dto.investment.HoldingDetailResponse;
import com.minilands.backend.dto.investment.SellSharesRequest;
import com.minilands.backend.dto.investment.SharePriceResponse;
import com.minilands.backend.service.investment.PropertyInvestmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final PropertyInvestmentService propertyInvestmentService;

    public InvestmentController(PropertyInvestmentService propertyInvestmentService) {
        this.propertyInvestmentService = propertyInvestmentService;
    }

    @GetMapping("/properties/{propertyId}/share-price")
    public ResponseEntity<SharePriceResponse> getSharePrice(@PathVariable String propertyId) {
        return ResponseEntity.ok(propertyInvestmentService.getSharePrice(propertyId));
    }

    @PostMapping("/buy")
    public ResponseEntity<HoldingDetailResponse> buyShares(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BuySharesRequest request) {
        return ResponseEntity.ok(propertyInvestmentService.buyShares(principal.getUserId(), request));
    }

    @PostMapping("/sell")
    public ResponseEntity<HoldingDetailResponse> sellShares(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellSharesRequest request) {
        return ResponseEntity.ok(propertyInvestmentService.sellShares(principal.getUserId(), request));
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingDetailResponse>> getHoldings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(propertyInvestmentService.getHoldings(principal.getUserId()));
    }

    @GetMapping("/holdings/{holdingId}")
    public ResponseEntity<HoldingDetailResponse> getHolding(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String holdingId) {
        return ResponseEntity.ok(propertyInvestmentService.getHolding(principal.getUserId(), holdingId));
    }
}
