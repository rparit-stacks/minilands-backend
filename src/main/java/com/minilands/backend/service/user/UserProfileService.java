package com.minilands.backend.service.user;

import com.minilands.backend.dto.user.UpdateProfileRequest;
import com.minilands.backend.dto.user.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(String userId);

    UserProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}
