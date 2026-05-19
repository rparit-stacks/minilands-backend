package com.minilands.backend.security;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.common.PrincipalType;
import com.minilands.backend.entity.Admin;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.exception.AuthException;
import com.minilands.backend.repository.AdminRepository;
import com.minilands.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   AdminRepository adminRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            setAuthError(request, "Missing Authorization header. Send: Authorization: Bearer <access-token>");
            filterChain.doFilter(request, response);
            return;
        }
        if (!header.startsWith("Bearer ")) {
            setAuthError(request,
                    "Invalid Authorization format. Use: Authorization: Bearer <access-token>");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            setAuthError(request, "Access token is empty. Send: Authorization: Bearer <access-token>");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            PrincipalType principalType = jwtService.extractPrincipalType(token);
            jwtService.validateAccessToken(token, principalType);

            if (principalType == PrincipalType.INVESTOR) {
                authenticateInvestor(token, request);
            } else {
                authenticateAdmin(token, request);
            }
        } catch (AuthException ex) {
            setAuthError(request, ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateInvestor(String token, HttpServletRequest request) {
        String userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is not active");
        }

        setAuthentication(new UserPrincipal(user.getId(), user.getEmail()), request);
    }

    private void authenticateAdmin(String token, HttpServletRequest request) {
        String adminId = jwtService.extractUserId(token);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AuthException("Admin not found"));

        if (admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is not active");
        }

        setAuthentication(new AdminPrincipal(admin.getId(), admin.getEmail()), request);
    }

    private void setAuthentication(UserDetails principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAuthError(HttpServletRequest request, String message) {
        request.setAttribute(SecurityAuthAttributes.AUTH_ERROR_MESSAGE, message);
    }
}
