package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.KycStatus;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 200) String name,
        @Size(max = 20) String phone,
        AccountStatus accountStatus,
        KycStatus kycStatus,
        @Size(max = 500) String kycRejectionNote
) {
}
