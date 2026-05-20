package com.minilands.backend.service.admin;

import com.minilands.backend.dto.admin.AdminResponse;
import com.minilands.backend.dto.admin.CreateAdminRequest;
import com.minilands.backend.dto.admin.UpdateAdminRequest;

import java.util.List;

public interface AdminManagementService {

    List<AdminResponse> listAdmins();

    AdminResponse getAdmin(String adminId);

    AdminResponse createAdmin(CreateAdminRequest request);

    AdminResponse updateAdmin(String adminId, UpdateAdminRequest request);

    void deleteAdmin(String requestingAdminId, String targetAdminId);
}
