package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyMediaItemDto(
        @NotNull MediaType mediaType,
        @NotBlank @Size(max = 2000) String mediaUrl,
        boolean primary,
        Integer displayOrder,
        @Size(max = 300) String caption
) {
}
