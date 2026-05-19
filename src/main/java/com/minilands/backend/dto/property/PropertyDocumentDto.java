package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.PropertyDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyDocumentDto(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String documentUrl,
        @NotNull PropertyDocumentType documentType
) {
}
