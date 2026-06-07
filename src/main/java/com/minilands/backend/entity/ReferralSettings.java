package com.minilands.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin-editable referral programme configuration. There is exactly one
 * document in this collection (id = {@link #SINGLETON_ID}); the
 * {@code ReferralSettingsService} loads-or-seeds it on demand.
 */
@Document(collection = "referral_settings")
public class ReferralSettings {

    /** Fixed id so the config is a singleton row. */
    public static final String SINGLETON_ID = "referral-settings";

    @Id
    private String id = SINGLETON_ID;

    /** Master switch — when false, no rewards are credited and the app hides the feature. */
    private boolean enabled = true;

    /**
     * Reward tiers, evaluated by the referrer's *successful* referral count.
     * The matching tier's {@code rewardPerReferral} is what the referrer earns
     * for the current referral. Tiers should be contiguous and ascending.
     */
    private List<ReferralTier> tiers = new ArrayList<>();

    /** One-time welcome bonus credited to the *referred friend* on their first investment. */
    private BigDecimal friendBonus = BigDecimal.ZERO;

    /** Currency for all referral amounts (kept in lock-step with the wallet). */
    private String currency = "INR";

    private Instant updatedAt;

    public ReferralSettings() {
    }

    /** A single reward bracket: referrals in [minReferrals, maxReferrals] earn {@code rewardPerReferral}. */
    public static class ReferralTier {
        /** Inclusive lower bound of the referrer's referral count (1-based). */
        private int minReferrals;
        /** Inclusive upper bound; null means "and above" (the top, open-ended tier). */
        private Integer maxReferrals;
        /** Flat amount the referrer earns per referral while in this bracket. */
        private BigDecimal rewardPerReferral;

        public ReferralTier() {
        }

        public ReferralTier(int minReferrals, Integer maxReferrals, BigDecimal rewardPerReferral) {
            this.minReferrals = minReferrals;
            this.maxReferrals = maxReferrals;
            this.rewardPerReferral = rewardPerReferral;
        }

        public int getMinReferrals() {
            return minReferrals;
        }

        public void setMinReferrals(int minReferrals) {
            this.minReferrals = minReferrals;
        }

        public Integer getMaxReferrals() {
            return maxReferrals;
        }

        public void setMaxReferrals(Integer maxReferrals) {
            this.maxReferrals = maxReferrals;
        }

        public BigDecimal getRewardPerReferral() {
            return rewardPerReferral;
        }

        public void setRewardPerReferral(BigDecimal rewardPerReferral) {
            this.rewardPerReferral = rewardPerReferral;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ReferralTier> getTiers() {
        return tiers;
    }

    public void setTiers(List<ReferralTier> tiers) {
        this.tiers = tiers;
    }

    public BigDecimal getFriendBonus() {
        return friendBonus;
    }

    public void setFriendBonus(BigDecimal friendBonus) {
        this.friendBonus = friendBonus;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
