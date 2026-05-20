package com.minilands.backend.service.user.impl;

import com.minilands.backend.dto.user.UpdateProfileRequest;
import com.minilands.backend.dto.user.UserProfileResponse;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.user.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    public UserProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileResponse getProfile(String userId) {
        User user = requireUser(userId);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        if (StringUtils.hasText(request.name())) {
            user.setName(request.name().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }
        if (request.profilePictureUrl() != null) {
            user.setProfilePictureUrl(
                    request.profilePictureUrl().isBlank() ? null : request.profilePictureUrl().trim());
        }

        user.setUpdatedAt(Instant.now());
        syncOnboardingCompleted(user);
        userRepository.save(user);
        return toResponse(user);
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // Onboarding is complete when name + phone are set AND KYC is approved.
    private void syncOnboardingCompleted(User user) {
        boolean completed = StringUtils.hasText(user.getName())
                && StringUtils.hasText(user.getPhone())
                && user.getKycStatus() == KycStatus.APPROVED;
        user.setOnboardingCompleted(completed);
    }

    private UserProfileResponse toResponse(User user) {
        syncOnboardingCompleted(user);
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getProfilePictureUrl(),
                user.getAuthProvider(),
                user.getKycStatus(),
                user.getAccountStatus(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.isOnboardingCompleted());
    }
}
