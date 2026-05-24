package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteAdminRequest(
        @NotBlank @Email String email,
        @NotNull AdminRole role
) {
}
