package com.minilands.backend.dto.auth;

public record RegisterRequest(
        String email,
        String password,
        String name,
        String phone
) {
}
