package com.minilands.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes the Firebase Admin SDK using the service-account JSON declared
 * in {@code app.fcm.credentials-path}.
 *
 * <p>Active only when push notifications are enabled — when disabled (e.g.
 * local dev without a Firebase project) we skip init entirely so the rest of
 * the app boots normally and the {@code NoOpMobilePushNotificationDelivery}
 * absorbs all push attempts.
 */
@Configuration
@ConditionalOnProperty(name = "app.notification.push-enabled", havingValue = "true")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.fcm.credentials-path}")
    private String credentialsPath;

    @Value("${app.fcm.project-id}")
    private String projectId;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initFirebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialised");
            return;
        }
        final Resource resource = resourceLoader.getResource(credentialsPath);
        if (!resource.exists()) {
            log.warn("Firebase credentials not found at {} — push will be a no-op", credentialsPath);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            final FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .setProjectId(projectId)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp initialised for project {}", projectId);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
