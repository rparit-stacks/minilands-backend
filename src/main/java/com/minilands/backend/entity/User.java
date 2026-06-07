package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AuthProvider;
import com.minilands.backend.entity.enums.KycStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String phone;
    private String name;

    @Indexed(unique = true, sparse = true)
    private String googleId;

    private AuthProvider authProvider;
    private KycStatus kycStatus;
    private AccountStatus accountStatus;
    private Instant emailVerifiedAt;
    private Instant kycVerifiedAt;
    private String kycRejectionNote;
    private String profilePictureUrl;
    private String oneSignalPlayerId;
    private boolean onboardingCompleted;

    /// The user's own shareable referral code (generated lazily on first fetch).
    @Indexed(unique = true, sparse = true)
    private String referralCode;

    /// The userId of whoever referred this user (null if they joined organically).
    private String referredByUserId;

    /// Set when this user's referral reward has been credited to their referrer,
    /// so we never double-pay. Acts as the "referral completed" marker.
    private Instant referralRewardedAt;

    private Instant createdAt;
    private Instant updatedAt;

    public User() {
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Instant getKycVerifiedAt() {
        return kycVerifiedAt;
    }

    public void setKycVerifiedAt(Instant kycVerifiedAt) {
        this.kycVerifiedAt = kycVerifiedAt;
    }

    public String getKycRejectionNote() {
        return kycRejectionNote;
    }

    public void setKycRejectionNote(String kycRejectionNote) {
        this.kycRejectionNote = kycRejectionNote;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getOneSignalPlayerId() {
        return oneSignalPlayerId;
    }

    public void setOneSignalPlayerId(String oneSignalPlayerId) {
        this.oneSignalPlayerId = oneSignalPlayerId;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public String getReferredByUserId() {
        return referredByUserId;
    }

    public void setReferredByUserId(String referredByUserId) {
        this.referredByUserId = referredByUserId;
    }

    public Instant getReferralRewardedAt() {
        return referralRewardedAt;
    }

    public void setReferralRewardedAt(Instant referralRewardedAt) {
        this.referralRewardedAt = referralRewardedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
