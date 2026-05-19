package com.minilands.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorWriter;

    public RestAccessDeniedHandler(SecurityErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        String message = resolveMessage(request);
        errorWriter.write(
                response,
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                request.getRequestURI());
    }

    private String resolveMessage(HttpServletRequest request) {
        Object attribute = request.getAttribute(SecurityAuthAttributes.AUTH_ERROR_MESSAGE);
        if (attribute instanceof String msg && !msg.isBlank()) {
            return msg;
        }

        String path = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (path.startsWith("/api/admin/")) {
            if (hasRole(authentication, "ROLE_INVESTOR")) {
                return "Admin access required. You are signed in as an investor. "
                        + "Use the access token from POST /api/admin/auth/login, not the investor OTP/Google token.";
            }
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return "Admin access required. Send header: Authorization: Bearer <admin-access-token> "
                        + "from POST /api/admin/auth/login.";
            }
            return "You do not have permission to access this admin resource.";
        }

        if (path.startsWith("/api/kyc/")
                || path.startsWith("/api/notifications/")
                || path.startsWith("/api/media/")
                || path.startsWith("/api/wallet/")) {
            if (hasRole(authentication, "ROLE_ADMIN")) {
                return "Investor access required. Use an investor access token from OTP or Google sign-in, "
                        + "not the admin login token.";
            }
            return "Investor access required. Send header: Authorization: Bearer <investor-access-token>.";
        }

        return "You do not have permission to perform this action.";
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
