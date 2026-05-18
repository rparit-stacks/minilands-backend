package com.minilands.backend.security;

public record GoogleUserInfo(
        String googleId,
        String email,
        String name
) {
}
