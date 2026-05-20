package com.minilands.backend.dto.property;

import java.util.List;

public record PropertyPageResponse(
        List<PropertySummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
