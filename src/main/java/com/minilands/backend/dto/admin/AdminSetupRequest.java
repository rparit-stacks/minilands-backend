package com.minilands.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public request from the setup page: new admin completes their profile by setting name + password. */
public record AdminSetupRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
