package com.minilands.backend.dto.wallet;

import jakarta.validation.constraints.Size;

public record AdminActionRequest(
        @Size(max = 500) String note
) {
}
