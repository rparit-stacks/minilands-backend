package com.minilands.backend.service.admin;

import com.minilands.backend.dto.admin.AdminUserResponse;
import com.minilands.backend.dto.admin.UpdateUserRequest;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.KycStatus;

import java.util.List;

public interface AdminUserManagementService {

    List<AdminUserResponse> listUsers(AccountStatus accountStatus, KycStatus kycStatus);

    AdminUserResponse getUser(String userId);

    AdminUserResponse updateUser(String userId, UpdateUserRequest request);

    void deleteUser(String userId);
}
