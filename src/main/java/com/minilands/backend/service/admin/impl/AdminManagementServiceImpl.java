package com.minilands.backend.service.admin.impl;

import com.minilands.backend.dto.admin.AdminResponse;
import com.minilands.backend.dto.admin.AdminSetupRequest;
import com.minilands.backend.dto.admin.CreateAdminRequest;
import com.minilands.backend.dto.admin.InviteAdminRequest;
import com.minilands.backend.dto.admin.InviteAdminResponse;
import com.minilands.backend.dto.admin.InviteInfoResponse;
import com.minilands.backend.dto.admin.UpdateAdminRequest;
import com.minilands.backend.entity.Admin;
import com.minilands.backend.entity.AdminInviteToken;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.repository.AdminInviteTokenRepository;
import com.minilands.backend.repository.AdminRepository;
import com.minilands.backend.security.TokenHashService;
import com.minilands.backend.service.admin.AdminInviteEmailService;
import com.minilands.backend.service.admin.AdminManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final long INVITE_TTL_HOURS = 72;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminRepository adminRepository;
    private final AdminInviteTokenRepository inviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashService tokenHashService;
    private final AdminInviteEmailService inviteEmailService;
    private final String dashboardBaseUrl;

    public AdminManagementServiceImpl(
            AdminRepository adminRepository,
            AdminInviteTokenRepository inviteRepository,
            PasswordEncoder passwordEncoder,
            TokenHashService tokenHashService,
            AdminInviteEmailService inviteEmailService,
            @Value("${app.admin.dashboard-url:http://localhost:3000}") String dashboardBaseUrl) {
        this.adminRepository = adminRepository;
        this.inviteRepository = inviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHashService = tokenHashService;
        this.inviteEmailService = inviteEmailService;
        this.dashboardBaseUrl = dashboardBaseUrl;
    }

    @Override
    public List<AdminResponse> listAdmins() {
        return adminRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminResponse getAdmin(String adminId) {
        return toResponse(findAdmin(adminId));
    }

    @Override
    public AdminResponse createAdmin(CreateAdminRequest request) {
        if (adminRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Admin with this email already exists");
        }
        Instant now = Instant.now();
        Admin admin = new Admin();
        admin.setEmail(request.email().toLowerCase().trim());
        admin.setName(request.name());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(request.role());
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        return toResponse(adminRepository.save(admin));
    }

    @Override
    public AdminResponse updateAdmin(String adminId, UpdateAdminRequest request) {
        Admin admin = findAdmin(adminId);
        if (request.name() != null) {
            admin.setName(request.name());
        }
        if (request.role() != null) {
            admin.setRole(request.role());
        }
        if (request.accountStatus() != null) {
            admin.setAccountStatus(request.accountStatus());
        }
        if (request.password() != null) {
            admin.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        admin.setUpdatedAt(Instant.now());
        return toResponse(adminRepository.save(admin));
    }

    @Override
    public void deleteAdmin(String requestingAdminId, String targetAdminId) {
        if (requestingAdminId.equals(targetAdminId)) {
            throw new IllegalArgumentException("Cannot delete your own admin account");
        }
        Admin admin = findAdmin(targetAdminId);
        adminRepository.delete(admin);
    }

    @Override
    public InviteAdminResponse inviteAdmin(String invitedByAdminId, InviteAdminRequest request) {
        String email = request.email().toLowerCase().trim();
        if (adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An admin with this email already exists");
        }

        String rawToken = generateRawToken();
        String tokenHash = tokenHashService.hash(rawToken);
        Instant now = Instant.now();

        AdminInviteToken invite = new AdminInviteToken();
        invite.setEmail(email);
        invite.setTokenHash(tokenHash);
        invite.setRole(request.role());
        invite.setInvitedByAdminId(invitedByAdminId);
        invite.setExpiresAt(now.plus(INVITE_TTL_HOURS, ChronoUnit.HOURS));
        invite.setCreatedAt(now);
        inviteRepository.save(invite);

        String setupUrl = buildSetupUrl(rawToken);
        boolean emailSent = inviteEmailService.sendInvite(email, request.role().name(), setupUrl, INVITE_TTL_HOURS);

        return new InviteAdminResponse(email, request.role(), setupUrl, invite.getExpiresAt(), emailSent);
    }

    @Override
    public InviteInfoResponse getInviteInfo(String rawToken) {
        AdminInviteToken invite = requireValidInvite(rawToken);
        return new InviteInfoResponse(invite.getEmail(), invite.getRole(), invite.getExpiresAt());
    }

    @Override
    public AdminResponse completeSetup(AdminSetupRequest request) {
        AdminInviteToken invite = requireValidInvite(request.token());

        if (adminRepository.existsByEmail(invite.getEmail())) {
            // An admin row already exists for this email — token shouldn't be active in that case,
            // but treat defensively by consuming the invite and erroring.
            invite.setConsumedAt(Instant.now());
            inviteRepository.save(invite);
            throw new IllegalArgumentException("An admin with this email already exists");
        }

        Instant now = Instant.now();
        Admin admin = new Admin();
        admin.setEmail(invite.getEmail());
        admin.setName(request.name().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(invite.getRole());
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        Admin saved = adminRepository.save(admin);

        invite.setConsumedAt(now);
        inviteRepository.save(invite);

        return toResponse(saved);
    }

    private AdminInviteToken requireValidInvite(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid setup link");
        }
        String tokenHash = tokenHashService.hash(rawToken);
        AdminInviteToken invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired setup link"));
        if (invite.getConsumedAt() != null) {
            throw new IllegalArgumentException("This setup link has already been used");
        }
        if (invite.getExpiresAt() != null && Instant.now().isAfter(invite.getExpiresAt())) {
            throw new IllegalArgumentException("This setup link has expired — ask for a new invite");
        }
        return invite;
    }

    private String generateRawToken() {
        byte[] buf = new byte[32];
        SECURE_RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String buildSetupUrl(String rawToken) {
        String base = dashboardBaseUrl == null ? "" : dashboardBaseUrl.replaceAll("/+$", "");
        return base + "/admin-setup?token=" + rawToken;
    }

    private Admin findAdmin(String adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
    }

    private AdminResponse toResponse(Admin admin) {
        return new AdminResponse(
                admin.getId(),
                admin.getEmail(),
                admin.getName(),
                admin.getRole(),
                admin.getAccountStatus(),
                admin.getLastLoginAt(),
                admin.getCreatedAt(),
                admin.getUpdatedAt());
    }
}
