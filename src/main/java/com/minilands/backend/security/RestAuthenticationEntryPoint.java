package com.minilands.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorWriter;

    public RestAuthenticationEntryPoint(SecurityErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String message = resolveMessage(request);
        errorWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                request.getRequestURI());
    }

    private String resolveMessage(HttpServletRequest request) {
        Object attribute = request.getAttribute(SecurityAuthAttributes.AUTH_ERROR_MESSAGE);
        if (attribute instanceof String msg && !msg.isBlank()) {
            return msg;
        }
        return "Authentication required. Send header: Authorization: Bearer <access-token>. "
                + "For admin APIs, obtain the token from POST /api/admin/auth/login.";
    }
}
