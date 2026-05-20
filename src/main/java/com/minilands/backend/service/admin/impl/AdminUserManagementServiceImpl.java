package com.minilands.backend.service.admin.impl;

import com.minilands.backend.dto.admin.AdminUserResponse;
import com.minilands.backend.dto.admin.UpdateUserRequest;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.admin.AdminUserManagementService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private final UserRepository userRepository;

    public AdminUserManagementServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<AdminUserResponse> listUsers(AccountStatus accountStatus, KycStatus kycStatus) {
        List<User> users;
        if (kycStatus != null) {
            users = userRepository.findByKycStatus(kycStatus);
        } else if (accountStatus != null) {
            users = userRepository.findByAccountStatus(accountStatus);
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminUserResponse getUser(String userId) {
        return toResponse(findUser(userId));
    }

    @Override
    public AdminUserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = findUser(userId);
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.accountStatus() != null) {
            user.setAccountStatus(request.accountStatus());
        }
        if (request.kycStatus() != null) {
            user.setKycStatus(request.kycStatus());
            if (request.kycStatus() == KycStatus.APPROVED) {
                user.setKycVerifiedAt(Instant.now());
                user.setKycRejectionNote(null);
            } else if (request.kycStatus() == KycStatus.REJECTED) {
                user.setKycRejectionNote(request.kycRejectionNote());
            }
        }
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(String userId) {
        User user = findUser(userId);
        userRepository.delete(user);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getProfilePictureUrl(),
                user.getAuthProvider(),
                user.getKycStatus(),
                user.getAccountStatus(),
                user.getEmailVerifiedAt(),
                user.getKycVerifiedAt(),
                user.getKycRejectionNote(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
