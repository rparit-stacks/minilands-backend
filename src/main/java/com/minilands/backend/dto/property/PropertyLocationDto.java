package com.minilands.backend.dto.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PropertyLocationDto(
        @NotBlank @Size(max = 300) String addressLine1,
        @Size(max = 300) String addressLine2,
        @Size(max = 120) String locality,
        @Size(max = 120) String landmark,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Size(max = 100) String country,
        @Size(max = 20) String postalCode,
        Double latitude,
        Double longitude
) {
}
