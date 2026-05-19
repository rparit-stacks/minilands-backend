package com.minilands.backend.config;

import com.minilands.backend.entity.Admin;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AdminRole;
import com.minilands.backend.repository.AdminRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties bootstrapProperties;

    public AdminBootstrapRunner(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            AdminBootstrapProperties bootstrapProperties) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapProperties.isEnabled()) {
            return;
        }

        String email = bootstrapProperties.getEmail().trim().toLowerCase();
        if (adminRepository.existsByEmail(email)) {
            return;
        }

        Instant now = Instant.now();
        Admin admin = new Admin();
        admin.setEmail(email);
        admin.setName(bootstrapProperties.getName());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapProperties.getPassword()));
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        adminRepository.save(admin);
    }
}
