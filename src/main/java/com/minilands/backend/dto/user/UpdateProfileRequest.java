package com.minilands.backend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 200) String name,
        @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number") String phone,
        @Size(max = 1000) String profilePictureUrl
) {
}
