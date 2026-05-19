/**
 * Notifications: in-app inbox + pluggable outbound delivery.
 * <ul>
 *   <li>{@link com.minilands.backend.service.notification.NotificationService} — facade</li>
 *   <li>{@link com.minilands.backend.service.notification.delivery.EmailNotificationDelivery} — SMTP</li>
 *   <li>{@link com.minilands.backend.service.notification.delivery.MobilePushNotificationDelivery} — push (future)</li>
 * </ul>
 */
package com.minilands.backend.service.notification;
