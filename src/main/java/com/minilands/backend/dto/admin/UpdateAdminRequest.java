package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AdminRole;
import jakarta.validation.constraints.Size;

public record UpdateAdminRequest(
        @Size(max = 200) String name,
        AdminRole role,
        AccountStatus accountStatus,
        @Size(min = 8, max = 100) String password
) {
}
