package com.minilands.backend.service.notification.impl;

import com.minilands.backend.config.NotificationProperties;
import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.dto.notification.NotificationResponse;
import com.minilands.backend.dto.notification.RegisterPushDeviceRequest;
import com.minilands.backend.entity.Notification;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.repository.NotificationRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final NotificationProperties notificationProperties;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            NotificationDeliveryOrchestrator deliveryOrchestrator,
            NotificationProperties notificationProperties) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
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
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Notification skipped — no user for userId={}", userId);
            return;
        }

        if (notificationProperties.isInAppEnabled()) {
            persistInApp(userId, type, title, message);
        }

        deliveryOrchestrator.deliverOutbound(new NotificationMessage(
                userId,
                user.getEmail(),
                user.getOneSignalPlayerId(),
                type,
                title,
                message));
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
