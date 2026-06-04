package com.minilands.backend.service.notification.impl;

import com.minilands.backend.config.NotificationProperties;
import com.minilands.backend.dto.notification.BroadcastNotificationRequest;
import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.dto.notification.NotificationResponse;
import com.minilands.backend.dto.notification.RegisterPushDeviceRequest;
import com.minilands.backend.entity.Notification;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.repository.NotificationRepository;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PropertyHoldingRepository propertyHoldingRepository;
    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final NotificationProperties notificationProperties;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            PropertyHoldingRepository propertyHoldingRepository,
            NotificationDeliveryOrchestrator deliveryOrchestrator,
            NotificationProperties notificationProperties) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.propertyHoldingRepository = propertyHoldingRepository;
        this.deliveryOrchestrator = deliveryOrchestrator;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> getUnread(String userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void send(String userId, NotificationType type, String title, String message) {
        sendRich(userId, type, title, message, null, null, Map.of());
    }

    @Override
    @Transactional
    public void sendRich(
            String userId,
            NotificationType type,
            String title,
            String message,
            String imageUrl,
            String deepLink,
            Map<String, String> data) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Notification skipped — no user for userId={}", userId);
            return;
        }

        if (notificationProperties.isInAppEnabled()) {
            persistInApp(userId, type, title, message);
        }

        // Inject deepLink into metadata so the client can read it from
        // `message.data['deepLink']` whether we shipped it as a top-level
        // field or in the bag.
        final Map<String, String> meta = new HashMap<>(data == null ? Map.of() : data);
        if (deepLink != null && !deepLink.isBlank()) {
            meta.putIfAbsent("deepLink", deepLink);
        }

        deliveryOrchestrator.deliverOutbound(new NotificationMessage(
                userId,
                user.getEmail(),
                user.getOneSignalPlayerId(),
                type,
                title,
                message,
                imageUrl,
                deepLink,
                Collections.unmodifiableMap(meta)));
    }

    @Override
    @Transactional
    public int broadcast(BroadcastNotificationRequest request) {
        final List<String> userIds = resolveTargetUserIds(request);
        if (userIds.isEmpty()) {
            log.info("Broadcast skipped — no recipients matched target={}", request.targetType());
            return 0;
        }

        log.info("Broadcasting notification target={} recipients={} title='{}'",
                request.targetType(), userIds.size(), request.title());

        for (String uid : userIds) {
            try {
                sendRich(
                        uid,
                        request.type(),
                        request.title(),
                        request.message(),
                        request.imageUrl(),
                        request.deepLink(),
                        request.data() == null ? Map.of() : request.data());
            } catch (Exception e) {
                log.warn("Broadcast failed for userId={}: {}", uid, e.getMessage());
            }
        }
        return userIds.size();
    }

    /// Resolves the broadcast `targetType` to a concrete list of user IDs.
    private List<String> resolveTargetUserIds(BroadcastNotificationRequest request) {
        return switch (request.targetType()) {
            case ALL_USERS -> userRepository.findByAccountStatus(AccountStatus.ACTIVE).stream()
                    .map(User::getId)
                    .toList();
            case KYC_APPROVED -> userRepository.findByKycStatus(KycStatus.APPROVED).stream()
                    .map(User::getId)
                    .toList();
            case KYC_PENDING -> {
                final List<User> pending = userRepository.findByKycStatus(KycStatus.PENDING);
                final List<User> rejected = userRepository.findByKycStatus(KycStatus.REJECTED);
                final Set<String> ids = new HashSet<>();
                pending.forEach(u -> ids.add(u.getId()));
                rejected.forEach(u -> ids.add(u.getId()));
                yield ids.stream().toList();
            }
            case INVESTORS -> propertyHoldingRepository.findAll().stream()
                    .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                    .map(PropertyHolding::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            case USER_IDS -> {
                if (request.userIds() == null || request.userIds().isEmpty()) {
                    throw new IllegalArgumentException(
                            "userIds is required when targetType=USER_IDS");
                }
                yield request.userIds();
            }
        };
    }

    @Override
    @Transactional
    public void registerPushDevice(String userId, RegisterPushDeviceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setOneSignalPlayerId(request.playerId().trim());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private void persistInApp(String userId, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
