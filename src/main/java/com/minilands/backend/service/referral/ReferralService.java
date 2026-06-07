package com.minilands.backend.service.referral;

import com.minilands.backend.config.ReferralProperties;
import com.minilands.backend.dto.referral.ReferralSummaryResponse;
import com.minilands.backend.dto.referral.ReferredUserResponse;
import com.minilands.backend.entity.ReferralSettings;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Core referral programme logic: code generation, the investor-facing summary,
 * linking a referral on signup, and crediting both sides once the referred
 * friend completes their first investment.
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    /** Excludes ambiguous chars (0/O, 1/I) so codes are easy to read aloud. */
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int MAX_CODE_ATTEMPTS = 8;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final WalletLedgerService walletLedgerService;
    private final NotificationService notificationService;
    private final ReferralSettingsService settingsService;
    private final ReferralProperties referralProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ReferralService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            WalletLedgerService walletLedgerService,
            NotificationService notificationService,
            ReferralSettingsService settingsService,
            ReferralProperties referralProperties) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.walletLedgerService = walletLedgerService;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.referralProperties = referralProperties;
    }

    // ── Investor-facing ────────────────────────────────────────────────────

    /** Builds the full Refer & Earn summary, generating the user's code if needed. */
    @Transactional
    public ReferralSummaryResponse getSummary(String userId) {
        User user = requireUser(userId);
        ReferralSettings settings = settingsService.getOrSeed();
        String code = ensureReferralCode(user);

        List<User> referred = userRepository.findByReferredByUserId(userId);
        int total = referred.size();
        int rewarded = (int) referred.stream().filter(u -> u.getReferralRewardedAt() != null).count();

        BigDecimal totalEarned = referralEarnings(userId);
        BigDecimal currentReward = settingsService.rewardForReferralCount(settings, rewarded + 1);

        return new ReferralSummaryResponse(
                settings.isEnabled(),
                code,
                buildLink(code),
                total,
                rewarded,
                totalEarned,
                settings.getCurrency(),
                currentReward,
                settings.getFriendBonus(),
                settingsService.tierSummary(settings));
    }

    /** Lists the friends this user has referred, newest first, with masked names. */
    public List<ReferredUserResponse> getReferredUsers(String userId) {
        return userRepository.findByReferredByUserId(userId).stream()
                .sorted(Comparator.comparing(
                        User::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(u -> new ReferredUserResponse(
                        maskName(u),
                        u.getReferralRewardedAt() != null ? "REWARDED" : "PENDING",
                        u.getCreatedAt(),
                        u.getReferralRewardedAt() != null ? rewardEarnedFor(userId, u.getId()) : BigDecimal.ZERO))
                .toList();
    }

    // ── Signup linking ─────────────────────────────────────────────────────

    /**
     * Links {@code newUser} to whoever owns {@code referralCode}. Safe to call
     * with a null/blank/unknown code (it's a no-op) and refuses self-referral
     * or re-linking. The actual reward is deferred to first investment.
     */
    @Transactional
    public void applyReferralOnSignup(User newUser, String referralCode) {
        if (referralCode == null || referralCode.isBlank()) {
            return;
        }
        if (newUser.getReferredByUserId() != null) {
            return; // already linked
        }
        Optional<User> referrer = userRepository.findByReferralCode(normalize(referralCode));
        if (referrer.isEmpty()) {
            log.info("Referral code '{}' did not match any user — ignoring for userId={}",
                    referralCode, newUser.getId());
            return;
        }
        if (referrer.get().getId().equals(newUser.getId())) {
            return; // can't refer yourself
        }
        newUser.setReferredByUserId(referrer.get().getId());
        userRepository.save(newUser);
        log.info("Linked userId={} as referred by userId={}", newUser.getId(), referrer.get().getId());
    }

    // ── Reward trigger ───────────────────────────────────────────────────────

    /**
     * Called once when a user completes their first investment. If they were
     * referred and haven't been rewarded yet, credits the referrer's tier reward
     * and the friend's welcome bonus, then notifies both. Best-effort: any
     * failure is logged but never blocks the investment that triggered it.
     */
    @Transactional
    public void rewardFirstInvestment(String investorUserId) {
        try {
            User friend = userRepository.findById(investorUserId).orElse(null);
            if (friend == null || friend.getReferredByUserId() == null
                    || friend.getReferralRewardedAt() != null) {
                return;
            }
            ReferralSettings settings = settingsService.getOrSeed();
            if (!settings.isEnabled()) {
                return;
            }
            User referrer = userRepository.findById(friend.getReferredByUserId()).orElse(null);
            if (referrer == null) {
                return;
            }

            // Mark first so a retry / concurrent call can't double-pay.
            friend.setReferralRewardedAt(Instant.now());
            userRepository.save(friend);

            int rewardedSoFar = (int) userRepository.findByReferredByUserId(referrer.getId()).stream()
                    .filter(u -> u.getReferralRewardedAt() != null)
                    .count(); // includes `friend` we just marked
            BigDecimal referrerReward = settingsService.rewardForReferralCount(settings, rewardedSoFar);

            creditReferrer(referrer, referrerReward, friend);
            creditFriendBonus(friend, settings.getFriendBonus());
        } catch (Exception e) {
            // Never let a referral payout failure break the investment flow.
            log.error("Referral reward failed for investorUserId={}: {}", investorUserId, e.getMessage(), e);
        }
    }

    private void creditReferrer(User referrer, BigDecimal amount, User friend) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Wallet wallet = walletLedgerService.requireWallet(referrer.getId());
        walletLedgerService.credit(
                wallet,
                amount,
                TransactionType.ADMIN_CREDIT,
                "Referral bonus — " + maskName(friend) + " made their first investment",
                "referral:" + friend.getId());
        notificationService.sendRich(
                referrer.getId(),
                NotificationType.REFERRAL,
                "Referral bonus earned! 🎉",
                "You earned ₹" + amount.stripTrailingZeros().toPlainString()
                        + " because " + maskName(friend) + " made their first investment.",
                null,
                "/wallet",
                java.util.Map.of());
    }

    private void creditFriendBonus(User friend, BigDecimal friendBonus) {
        if (friendBonus == null || friendBonus.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Wallet wallet = walletLedgerService.requireWallet(friend.getId());
        walletLedgerService.credit(
                wallet,
                friendBonus,
                TransactionType.ADMIN_CREDIT,
                "Welcome referral bonus",
                "referral-welcome:" + friend.getId());
        notificationService.sendRich(
                friend.getId(),
                NotificationType.REFERRAL,
                "Welcome bonus credited! 🎁",
                "₹" + friendBonus.stripTrailingZeros().toPlainString()
                        + " has been added to your wallet for joining via a referral.",
                null,
                "/wallet",
                java.util.Map.of());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns the user's referral code, generating and persisting one if absent. */
    @Transactional
    public String ensureReferralCode(User user) {
        if (user.getReferralCode() != null && !user.getReferralCode().isBlank()) {
            return user.getReferralCode();
        }
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String candidate = generateCode();
            if (userRepository.existsByReferralCode(candidate)) {
                continue;
            }
            user.setReferralCode(candidate);
            try {
                userRepository.save(user);
                return candidate;
            } catch (DuplicateKeyException dup) {
                // Lost a race on the unique index — retry with a fresh code.
                user.setReferralCode(null);
            }
        }
        throw new IllegalStateException("Could not generate a unique referral code");
    }

    private String buildLink(String code) {
        String base = referralProperties.getLinkBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + code;
    }

    private String generateCode() {
        int len = Math.max(4, referralProperties.getCodeLength());
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    /** Sum of referral credits this user has received (by reference-id prefix). */
    private BigDecimal referralEarnings(String userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(t -> t.getReferenceId() != null && t.getReferenceId().startsWith("referral:"))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rewardEarnedFor(String referrerUserId, String friendUserId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(referrerUserId).stream()
                .filter(t -> ("referral:" + friendUserId).equals(t.getReferenceId()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String maskName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            String local = email.substring(0, email.indexOf('@'));
            if (local.length() <= 2) {
                return local.charAt(0) + "***";
            }
            return local.substring(0, 2) + "***";
        }
        return "A friend";
    }

    private String normalize(String code) {
        return code.trim().toUpperCase();
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
