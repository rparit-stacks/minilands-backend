package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.AdminRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One-time invite token for new admin onboarding. The raw token is emailed to the invitee;
 * we only store its SHA-256 hash. Consumed once the invitee finishes setup (sets a password).
 */
@Document(collection = "admin_invite_tokens")
public class AdminInviteToken {

    @Id
    private String id;

    @Indexed
    private String email;

    /** SHA-256 hash of the raw token sent by email. */
    @Indexed(unique = true)
    private String tokenHash;

    private AdminRole role;

    /** Admin id that issued the invite (for audit). */
    private String invitedByAdminId;

    private Instant expiresAt;
    private Instant consumedAt;
    private Instant createdAt;

    public AdminInviteToken() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }

    public String getInvitedByAdminId() {
        return invitedByAdminId;
    }

    public void setInvitedByAdminId(String invitedByAdminId) {
        this.invitedByAdminId = invitedByAdminId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
