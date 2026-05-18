package com.minilands.backend.dto.auth;

public record LoginRequest(
        String email,
        String password
) {
}
