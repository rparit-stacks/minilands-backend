package com.minilands.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OneSignal REST API credentials — used when {@link PushNotificationSender} implementation is added.
 */
@ConfigurationProperties(prefix = "app.onesignal")
public class OneSignalProperties {

    private String appId;
    private String restApiKey;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getRestApiKey() {
        return restApiKey;
    }

    public void setRestApiKey(String restApiKey) {
        this.restApiKey = restApiKey;
    }

    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && restApiKey != null && !restApiKey.isBlank();
    }
}
