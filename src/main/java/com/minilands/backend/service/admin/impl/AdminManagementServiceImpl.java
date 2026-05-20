package com.minilands.backend.service.admin.impl;

import com.minilands.backend.dto.admin.AdminResponse;
import com.minilands.backend.dto.admin.CreateAdminRequest;
import com.minilands.backend.dto.admin.UpdateAdminRequest;
import com.minilands.backend.entity.Admin;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.repository.AdminRepository;
import com.minilands.backend.service.admin.AdminManagementService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminManagementServiceImpl(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
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
