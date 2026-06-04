package com.minilands.backend.service.notification.delivery.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.minilands.backend.dto.notification.NotificationMessage;
import com.minilands.backend.service.notification.delivery.MobilePushNotificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Real FCM-backed push delivery. Active only when
 * {@code app.notification.push-enabled=true}; the {@link NoOpMobilePushNotificationDelivery}
 * stays in scope when disabled so Spring still has a bean to autowire.
 *
 * <p>The user's FCM device token rides in {@link NotificationMessage#mobilePushPlayerId()}
 * — the field name is legacy (OneSignal era) but semantically it's now the FCM token.
 */
@Service
@ConditionalOnProperty(name = "app.notification.push-enabled", havingValue = "true")
public class FcmMobilePushNotificationDelivery implements MobilePushNotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(FcmMobilePushNotificationDelivery.class);

    /// Android notification channel ID — must match the one the Flutter app creates.
    private static final String CHANNEL_ID = "minilands_default";

    private final FirebaseMessaging firebaseMessaging;

    public FcmMobilePushNotificationDelivery(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void deliver(NotificationMessage message) {
        final String token = message.mobilePushPlayerId();
        if (token == null || token.isBlank()) {
            log.debug("Skipping push for user {} — no device token", message.userId());
            return;
        }

        // Top-level notification (shown in tray). Attach the image URL so
        // Android renders it as a big-picture style notification.
        final Notification.Builder nbuilder = Notification.builder()
                .setTitle(message.title())
                .setBody(message.body());
        if (message.imageUrl() != null && !message.imageUrl().isBlank()) {
            nbuilder.setImage(message.imageUrl());
        }

        // Tag the payload with the in-app notification type, deepLink, and any
        // extra metadata so the client can deep-link on tap.
        final Map<String, String> data = new HashMap<>();
        if (message.type() != null) data.put("type", message.type().name());
        if (message.deepLink() != null && !message.deepLink().isBlank()) {
            data.put("deepLink", message.deepLink());
        }
        if (message.metadata() != null) data.putAll(message.metadata());

        // Android-specific big-picture / inbox style when an image is present.
        final AndroidNotification.Builder androidBuilder = AndroidNotification.builder()
                .setChannelId(CHANNEL_ID)
                .setIcon("ic_notification")
                .setColor("#FACC15");
        if (message.imageUrl() != null && !message.imageUrl().isBlank()) {
            androidBuilder.setImage(message.imageUrl());
        }

        final Message msg = Message.builder()
                .setToken(token)
                .setNotification(nbuilder.build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(androidBuilder.build())
                        .build())
                .build();

        try {
            final String messageId = firebaseMessaging.send(msg);
            log.debug("FCM push sent to user={} messageId={}", message.userId(), messageId);
        } catch (FirebaseMessagingException e) {
            // Stale / invalidated token? Don't fail loudly — log + move on. The
            // client will register a fresh token on next foreground.
            log.warn("FCM push failed for user={} code={} msg={}",
                    message.userId(), e.getMessagingErrorCode(), e.getMessage());
        }
    }
}
