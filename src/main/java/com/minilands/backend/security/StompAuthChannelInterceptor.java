package com.minilands.backend.security;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.common.PrincipalType;
import com.minilands.backend.entity.Admin;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.repository.AdminRepository;
import com.minilands.backend.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame using the same JWT the REST API uses.
 * The resolved {@link UserPrincipal}/{@link AdminPrincipal} is attached to the
 * STOMP session so {@code @MessageMapping} handlers can read it via the
 * {@link java.security.Principal} argument (and {@code principal.getName()}
 * returns the user/admin id).
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            AdminRepository adminRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // Only the CONNECT frame needs to authenticate; subsequent frames reuse
        // the user bound to the session below.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = bearerToken(accessor);
            if (token == null) {
                throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
            }
            UserDetails principal = resolvePrincipal(token);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            accessor.setUser(auth);
        }
        return message;
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        List<String> auth = accessor.getNativeHeader("Authorization");
        if (auth == null || auth.isEmpty()) {
            return null;
        }
        String header = auth.get(0);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private UserDetails resolvePrincipal(String token) {
        PrincipalType principalType = jwtService.extractPrincipalType(token);
        jwtService.validateAccessToken(token, principalType);
        String id = jwtService.extractUserId(token);

        if (principalType == PrincipalType.INVESTOR) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Account is not active");
            }
            return new UserPrincipal(user.getId(), user.getEmail());
        }

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        if (admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
        return new AdminPrincipal(admin.getId(), admin.getEmail());
    }
}
