package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertyPageResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;
import com.minilands.backend.service.property.PropertyCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PropertyController.class);

    private final PropertyCatalogService propertyCatalogService;

    public PropertyController(PropertyCatalogService propertyCatalogService) {
        this.propertyCatalogService = propertyCatalogService;
    }

    @GetMapping("/search")
    public ResponseEntity<PropertyPageResponse> search(
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        log.info("[PropertyController] GET /api/properties/search — status={} type={} city={} featured={} q={} sortBy={} page={} size={}",
                status, type, city, featured, q, sortBy, page, size);
        PropertyPageResponse response = propertyCatalogService.search(status, type, city, featured, q, sortBy, page, size);
        log.info("[PropertyController] search ok — totalElements={} page={}", response.totalElements(), response.page());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PropertySummaryResponse>> list(
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean featured) {
        log.info("[PropertyController] GET /api/properties — status={} type={} city={} featured={}", status, type, city, featured);
        List<PropertySummaryResponse> result = propertyCatalogService.listAvailable(status, type, city, featured);
        log.info("[PropertyController] list ok — count={}", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyDetailResponse> getById(@PathVariable String propertyId) {
        log.info("[PropertyController] GET /api/properties/{}", propertyId);
        return ResponseEntity.ok(propertyCatalogService.getById(propertyId));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PropertyDetailResponse> getBySlug(@PathVariable String slug) {
        log.info("[PropertyController] GET /api/properties/slug/{}", slug);
        return ResponseEntity.ok(propertyCatalogService.getBySlug(slug));
    }
}
