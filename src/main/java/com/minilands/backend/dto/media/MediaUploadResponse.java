package com.minilands.backend.dto.media;

public record MediaUploadResponse(
        String url,
        String secureUrl,
        String publicId,
        String resourceType,
        String format,
        Long bytes,
        Integer width,
        Integer height
) {
}
