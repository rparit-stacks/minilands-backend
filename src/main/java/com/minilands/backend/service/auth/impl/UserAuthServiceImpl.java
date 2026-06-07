package com.minilands.backend.service.auth.impl;

import com.minilands.backend.config.JwtProperties;
import com.minilands.backend.config.OtpProperties;
import com.minilands.backend.dto.auth.AuthResponse;
import com.minilands.backend.dto.auth.GoogleAuthRequest;
import com.minilands.backend.dto.auth.RefreshTokenRequest;
import com.minilands.backend.dto.auth.SendOtpRequest;
import com.minilands.backend.dto.auth.VerifyOtpRequest;
import com.minilands.backend.dto.common.PrincipalType;
import com.minilands.backend.entity.EmailOtp;
import com.minilands.backend.entity.RefreshToken;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AuthProvider;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.exception.AuthException;
import com.minilands.backend.repository.EmailOtpRepository;
import com.minilands.backend.repository.RefreshTokenRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.security.GoogleTokenVerifierService;
import com.minilands.backend.security.GoogleUserInfo;
import com.minilands.backend.security.JwtService;
import com.minilands.backend.security.TokenHashService;
import com.minilands.backend.service.auth.OtpEmailService;
import com.minilands.backend.service.auth.UserAuthService;
import com.minilands.backend.service.referral.ReferralService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;
    private final JwtProperties jwtProperties;
    private final OtpEmailService otpEmailService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ReferralService referralService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserAuthServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            EmailOtpRepository emailOtpRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            TokenHashService tokenHashService,
            PasswordEncoder passwordEncoder,
            OtpProperties otpProperties,
            JwtProperties jwtProperties,
            OtpEmailService otpEmailService,
            GoogleTokenVerifierService googleTokenVerifierService,
            ReferralService referralService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.emailOtpRepository = emailOtpRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.otpProperties = otpProperties;
        this.jwtProperties = jwtProperties;
        this.otpEmailService = otpEmailService;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.referralService = referralService;
    }

    @Override
    public void sendEmailOtp(SendOtpRequest request) {
        String email = normalizeEmail(request.email());

        Optional<EmailOtp> existingOtp = emailOtpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(email);
        existingOtp.ifPresent(existing -> {
            long secondsSinceCreated = ChronoUnit.SECONDS.between(existing.getCreatedAt(), Instant.now());
            if (secondsSinceCreated < otpProperties.getResendCooldownSeconds()) {
                throw new IllegalArgumentException("Please wait before requesting another OTP");
            }
            existing.setConsumed(true);
            emailOtpRepository.save(existing);
        });

        String otp = generateOtp();
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtpHash(passwordEncoder.encode(otp));
        emailOtp.setAttempts(0);
        emailOtp.setConsumed(false);
        emailOtp.setExpiresAt(Instant.now().plus(otpProperties.getExpirationMinutes(), ChronoUnit.MINUTES));
        emailOtp.setCreatedAt(Instant.now());
        emailOtpRepository.save(emailOtp);

        otpEmailService.sendOtp(email, otp);
    }

    @Override
    public AuthResponse verifyEmailOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        EmailOtp emailOtp = emailOtpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AuthException("OTP not found. Request a new one."));

        validateOtpRecord(emailOtp);

        if (!passwordEncoder.matches(request.otp(), emailOtp.getOtpHash())) {
            emailOtp.setAttempts(emailOtp.getAttempts() + 1);
            emailOtpRepository.save(emailOtp);
            throw new AuthException("Invalid OTP");
        }

        emailOtp.setConsumed(true);
        emailOtpRepository.save(emailOtp);

        Optional<User> existing = userRepository.findByEmail(email);
        boolean isNewUser = existing.isEmpty();
        User user = existing.orElseGet(() -> createUser(email, null, null, AuthProvider.EMAIL_OTP));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new AuthException("This email is registered with Google. Use Google sign-in.");
        }

        user.setEmailVerifiedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        ensureWallet(user);

        if (isNewUser) {
            referralService.applyReferralOnSignup(user, request.referralCode());
        }

        return issueTokens(user);
    }

    @Override
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifierService.verify(request.idToken());

        Optional<User> existing = userRepository.findByGoogleId(googleUser.googleId())
                .or(() -> userRepository.findByEmail(googleUser.email()));
        boolean isNewUser = existing.isEmpty();
        User user = existing.orElseGet(() -> createUser(
                googleUser.email(),
                googleUser.name(),
                googleUser.googleId(),
                AuthProvider.GOOGLE));

        if (user.getAuthProvider() == AuthProvider.EMAIL_OTP && user.getGoogleId() == null) {
            user.setGoogleId(googleUser.googleId());
            user.setAuthProvider(AuthProvider.GOOGLE);
        } else if (user.getAuthProvider() == AuthProvider.EMAIL_OTP && user.getGoogleId() != null
                && !user.getGoogleId().equals(googleUser.googleId())) {
            throw new AuthException("Email already linked to a different Google account");
        }

        if (user.getName() == null && googleUser.name() != null) {
            user.setName(googleUser.name());
        }
        user.setGoogleId(googleUser.googleId());
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEmailVerifiedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        ensureWallet(user);

        if (isNewUser) {
            referralService.applyReferralOnSignup(user, request.referralCode());
        }

        return issueTokens(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.refreshToken();
        jwtService.validateRefreshToken(rawRefreshToken);

        String userId = jwtService.extractUserId(rawRefreshToken);
        RefreshToken stored = findActiveRefreshToken(userId, rawRefreshToken);

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is not active");
        }

        return issueTokens(user);
    }

    @Override
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private RefreshToken findActiveRefreshToken(String userId, String rawRefreshToken) {
        return refreshTokenRepository.findByUserId(userId).stream()
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .filter(token -> tokenHashService.matches(rawRefreshToken, token.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Invalid refresh token"));
    }

    private void validateOtpRecord(EmailOtp emailOtp) {
        if (emailOtp.isConsumed()) {
            throw new AuthException("OTP already used");
        }
        if (emailOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthException("OTP expired");
        }
        if (emailOtp.getAttempts() >= otpProperties.getMaxAttempts()) {
            throw new AuthException("Too many attempts. Request a new OTP");
        }
    }

    private AuthResponse issueTokens(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is not active");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        persistRefreshToken(user.getId(), refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessExpirationSeconds(),
                user.getId(),
                PrincipalType.INVESTOR);
    }

    private void persistRefreshToken(String userId, String rawRefreshToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHashService.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    private User createUser(String email, String name, String googleId, AuthProvider provider) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setGoogleId(googleId);
        user.setAuthProvider(provider);
        user.setKycStatus(KycStatus.PENDING);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    private void ensureWallet(User user) {
        if (walletRepository.findByUserId(user.getId()).isEmpty()) {
            Wallet wallet = new Wallet();
            wallet.setUserId(user.getId());
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setCurrency("INR");
            wallet.setUpdatedAt(Instant.now());
            walletRepository.save(wallet);
        }
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpProperties.getLength());
        int otp = secureRandom.nextInt(bound / 10, bound);
        return String.valueOf(otp);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
