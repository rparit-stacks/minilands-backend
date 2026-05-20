package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 200) String name,
        @NotNull AdminRole role
) {
}
