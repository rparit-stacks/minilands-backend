package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;
import com.minilands.backend.service.property.PropertyCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyCatalogService propertyCatalogService;

    public PropertyController(PropertyCatalogService propertyCatalogService) {
        this.propertyCatalogService = propertyCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<PropertySummaryResponse>> list(
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean featured) {
        return ResponseEntity.ok(propertyCatalogService.listAvailable(status, type, city, featured));
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyDetailResponse> getById(@PathVariable String propertyId) {
        return ResponseEntity.ok(propertyCatalogService.getById(propertyId));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PropertyDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(propertyCatalogService.getBySlug(slug));
    }
}
