package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.MediaType;

public record PropertyMediaResponse(
        String id,
        MediaType mediaType,
        String mediaUrl,
        boolean primary,
        Integer displayOrder,
        String caption
) {
}
