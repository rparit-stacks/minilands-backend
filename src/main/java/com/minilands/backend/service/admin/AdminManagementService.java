package com.minilands.backend.service.admin;

import com.minilands.backend.dto.admin.AdminResponse;
import com.minilands.backend.dto.admin.AdminSetupRequest;
import com.minilands.backend.dto.admin.CreateAdminRequest;
import com.minilands.backend.dto.admin.InviteAdminRequest;
import com.minilands.backend.dto.admin.InviteAdminResponse;
import com.minilands.backend.dto.admin.InviteInfoResponse;
import com.minilands.backend.dto.admin.UpdateAdminRequest;

import java.util.List;

public interface AdminManagementService {

    List<AdminResponse> listAdmins();

    AdminResponse getAdmin(String adminId);

    AdminResponse createAdmin(CreateAdminRequest request);

    AdminResponse updateAdmin(String adminId, UpdateAdminRequest request);

    void deleteAdmin(String requestingAdminId, String targetAdminId);

    /** Invite a new admin by email — issues a one-time setup token and emails the link. */
    InviteAdminResponse inviteAdmin(String invitedByAdminId, InviteAdminRequest request);

    /** Public — confirm a token is valid and return what email/role it was issued for. */
    InviteInfoResponse getInviteInfo(String rawToken);

    /** Public — complete admin setup with name + password using a valid invite token. */
    AdminResponse completeSetup(AdminSetupRequest request);
}
