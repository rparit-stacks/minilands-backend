package com.minilands.backend.security;

/**
 * Request attributes set by {@link JwtAuthenticationFilter} for security error handlers.
 */
public final class SecurityAuthAttributes {

    public static final String AUTH_ERROR_MESSAGE = "minilands.authErrorMessage";

    private SecurityAuthAttributes() {
    }
}
