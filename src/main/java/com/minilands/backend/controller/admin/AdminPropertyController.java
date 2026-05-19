package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.property.CreatePropertyRequest;
import com.minilands.backend.dto.property.DistributeRentRequest;
import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.dto.property.RentDistributionResponse;
import com.minilands.backend.dto.property.UpdatePropertyRequest;
import com.minilands.backend.dto.property.UpdatePropertyStatusRequest;
import com.minilands.backend.dto.property.UpdatePropertyValuationRequest;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.property.AdminPropertyCatalogService;
import com.minilands.backend.service.property.AdminPropertyValuationService;
import com.minilands.backend.service.property.PropertyRentDistributionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/properties")
public class AdminPropertyController {

    private final AdminPropertyCatalogService adminPropertyCatalogService;
    private final AdminPropertyValuationService adminPropertyValuationService;
    private final PropertyRentDistributionService propertyRentDistributionService;

    public AdminPropertyController(
            AdminPropertyCatalogService adminPropertyCatalogService,
            AdminPropertyValuationService adminPropertyValuationService,
            PropertyRentDistributionService propertyRentDistributionService) {
        this.adminPropertyCatalogService = adminPropertyCatalogService;
        this.adminPropertyValuationService = adminPropertyValuationService;
        this.propertyRentDistributionService = propertyRentDistributionService;
    }

    @PostMapping
    public ResponseEntity<PropertyDetailResponse> create(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreatePropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminPropertyCatalogService.create(principal.getAdminId(), request));
    }

    @GetMapping
    public ResponseEntity<List<PropertySummaryResponse>> list(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) PropertyStatus status) {
        return ResponseEntity.ok(adminPropertyCatalogService.listAll(status));
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyDetailResponse> getById(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId) {
        return ResponseEntity.ok(adminPropertyCatalogService.getById(propertyId));
    }

    @PutMapping("/{propertyId}")
    public ResponseEntity<PropertyDetailResponse> update(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @Valid @RequestBody UpdatePropertyRequest request) {
        return ResponseEntity.ok(adminPropertyCatalogService.update(propertyId, request));
    }

    @PatchMapping("/{propertyId}/status")
    public ResponseEntity<PropertyDetailResponse> updateStatus(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @Valid @RequestBody UpdatePropertyStatusRequest request) {
        return ResponseEntity.ok(adminPropertyCatalogService.updateStatus(propertyId, request));
    }

    @PatchMapping("/{propertyId}/valuation")
    public ResponseEntity<PropertyDetailResponse> updateValuation(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @Valid @RequestBody UpdatePropertyValuationRequest request) {
        return ResponseEntity.ok(adminPropertyValuationService.updateValuation(propertyId, request));
    }

    @PostMapping("/{propertyId}/rent/distribute")
    public ResponseEntity<RentDistributionResponse> distributeRent(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @Valid @RequestBody(required = false) DistributeRentRequest request) {
        DistributeRentRequest body = request != null ? request : new DistributeRentRequest(null, null);
        return ResponseEntity.ok(propertyRentDistributionService.distributeRent(propertyId, body));
    }
}
