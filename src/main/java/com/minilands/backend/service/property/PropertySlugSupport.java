package com.minilands.backend.service.property;

import com.minilands.backend.repository.PropertyRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class PropertySlugSupport {

    private final PropertyRepository propertyRepository;

    public PropertySlugSupport(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public String resolveUniqueSlug(String requestedSlug, String name, String excludePropertyId) {
        String base = StringUtils.hasText(requestedSlug)
                ? normalize(requestedSlug)
                : normalize(name);
        if (!StringUtils.hasText(base)) {
            base = "property";
        }

        String candidate = base;
        int suffix = 1;
        while (slugTaken(candidate, excludePropertyId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, String excludePropertyId) {
        if (!StringUtils.hasText(excludePropertyId)) {
            return propertyRepository.existsBySlug(slug);
        }
        return propertyRepository.existsBySlugAndIdNot(slug, excludePropertyId);
    }

    private String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
