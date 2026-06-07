package com.minilands.backend.service.referral;

import com.minilands.backend.dto.referral.ReferralSettingsDto;
import com.minilands.backend.dto.referral.ReferralTierDto;
import com.minilands.backend.entity.ReferralSettings;
import com.minilands.backend.repository.ReferralSettingsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads-or-seeds the singleton {@link ReferralSettings} document and exposes it
 * to both the runtime reward logic and the admin dashboard.
 */
@Service
public class ReferralSettingsService {

    private final ReferralSettingsRepository repository;

    public ReferralSettingsService(ReferralSettingsRepository repository) {
        this.repository = repository;
    }

    /** Returns the live settings, seeding sensible defaults the first time. */
    public ReferralSettings getOrSeed() {
        return repository.findById(ReferralSettings.SINGLETON_ID)
                .orElseGet(() -> repository.save(defaults()));
    }

    public ReferralSettingsDto getDto() {
        return toDto(getOrSeed());
    }

    /** Admin update — overwrites the editable fields and bumps {@code updatedAt}. */
    public ReferralSettingsDto update(ReferralSettingsDto dto) {
        ReferralSettings settings = getOrSeed();
        settings.setEnabled(dto.enabled());
        settings.setFriendBonus(dto.friendBonus() != null ? dto.friendBonus() : BigDecimal.ZERO);
        if (dto.currency() != null && !dto.currency().isBlank()) {
            settings.setCurrency(dto.currency().trim());
        }
        settings.setTiers(toEntityTiers(dto.tiers()));
        settings.setUpdatedAt(Instant.now());
        return toDto(repository.save(settings));
    }

    /**
     * Resolves the per-referral reward for a referrer who is about to record
     * their {@code newReferralCount}-th successful referral (1-based). Returns
     * {@link BigDecimal#ZERO} if no tier matches.
     */
    public BigDecimal rewardForReferralCount(ReferralSettings settings, int newReferralCount) {
        for (ReferralSettings.ReferralTier tier : settings.getTiers()) {
            boolean aboveMin = newReferralCount >= tier.getMinReferrals();
            boolean belowMax = tier.getMaxReferrals() == null || newReferralCount <= tier.getMaxReferrals();
            if (aboveMin && belowMax && tier.getRewardPerReferral() != null) {
                return tier.getRewardPerReferral();
            }
        }
        return BigDecimal.ZERO;
    }

    /** Short human-readable description of the tier ladder for the app hero copy. */
    public String tierSummary(ReferralSettings settings) {
        List<ReferralSettings.ReferralTier> tiers = settings.getTiers();
        if (tiers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tiers.size(); i++) {
            ReferralSettings.ReferralTier t = tiers.get(i);
            if (i > 0) {
                sb.append(" · ");
            }
            String range = t.getMaxReferrals() == null
                    ? t.getMinReferrals() + "+"
                    : t.getMinReferrals() + "–" + t.getMaxReferrals();
            sb.append(range).append(": ₹").append(stripTrailingZeros(t.getRewardPerReferral()));
        }
        return sb.toString();
    }

    private ReferralSettings defaults() {
        ReferralSettings settings = new ReferralSettings();
        settings.setEnabled(true);
        settings.setCurrency("INR");
        settings.setFriendBonus(new BigDecimal("100"));
        List<ReferralSettings.ReferralTier> tiers = new ArrayList<>();
        tiers.add(new ReferralSettings.ReferralTier(1, 5, new BigDecimal("100")));
        tiers.add(new ReferralSettings.ReferralTier(6, 15, new BigDecimal("200")));
        tiers.add(new ReferralSettings.ReferralTier(16, null, new BigDecimal("300")));
        settings.setTiers(tiers);
        settings.setUpdatedAt(Instant.now());
        return settings;
    }

    private List<ReferralSettings.ReferralTier> toEntityTiers(List<ReferralTierDto> dtos) {
        List<ReferralSettings.ReferralTier> tiers = new ArrayList<>();
        if (dtos == null) {
            return tiers;
        }
        for (ReferralTierDto d : dtos) {
            tiers.add(new ReferralSettings.ReferralTier(
                    d.minReferrals(),
                    d.maxReferrals(),
                    d.rewardPerReferral() != null ? d.rewardPerReferral() : BigDecimal.ZERO));
        }
        return tiers;
    }

    private ReferralSettingsDto toDto(ReferralSettings settings) {
        List<ReferralTierDto> tiers = settings.getTiers().stream()
                .map(t -> new ReferralTierDto(t.getMinReferrals(), t.getMaxReferrals(), t.getRewardPerReferral()))
                .toList();
        return new ReferralSettingsDto(
                settings.isEnabled(),
                tiers,
                settings.getFriendBonus(),
                settings.getCurrency(),
                settings.getUpdatedAt());
    }

    private String stripTrailingZeros(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
