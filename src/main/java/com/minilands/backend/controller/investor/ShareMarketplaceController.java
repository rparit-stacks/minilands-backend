package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.marketplace.BuyListedSharesRequest;
import com.minilands.backend.dto.marketplace.ListSharesRequest;
import com.minilands.backend.dto.marketplace.MarketplaceListingResponse;
import com.minilands.backend.service.marketplace.ShareMarketplaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class ShareMarketplaceController {

    private final ShareMarketplaceService shareMarketplaceService;

    public ShareMarketplaceController(ShareMarketplaceService shareMarketplaceService) {
        this.shareMarketplaceService = shareMarketplaceService;
    }

    @PostMapping("/listings")
    public ResponseEntity<MarketplaceListingResponse> listShares(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ListSharesRequest request) {
        return ResponseEntity.ok(shareMarketplaceService.listShares(principal.getUserId(), request));
    }

    @PostMapping("/listings/buy")
    public ResponseEntity<MarketplaceListingResponse> buyListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BuyListedSharesRequest request) {
        return ResponseEntity.ok(shareMarketplaceService.buyListing(principal.getUserId(), request));
    }

    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<MarketplaceListingResponse> cancelListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String listingId) {
        return ResponseEntity.ok(shareMarketplaceService.cancelListing(principal.getUserId(), listingId));
    }

    @GetMapping("/listings/properties/{propertyId}")
    public ResponseEntity<List<MarketplaceListingResponse>> getListingsForProperty(
            @PathVariable String propertyId) {
        return ResponseEntity.ok(shareMarketplaceService.getListingsForProperty(propertyId));
    }

    @GetMapping("/listings/mine")
    public ResponseEntity<List<MarketplaceListingResponse>> getMyListings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(shareMarketplaceService.getMyListings(principal.getUserId()));
    }
}
