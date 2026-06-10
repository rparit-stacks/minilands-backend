package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.ChatMessageType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One message in a property's group chat. There is one logical group per
 * property (keyed by {@link #propertyId}); we don't need a separate group
 * document. Soft-deleted by admins via {@link #deleted}.
 */
@Document(collection = "chat_messages")
@CompoundIndex(name = "property_created_idx", def = "{'propertyId': 1, 'createdAt': -1}")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String propertyId;

    /** Author. For admin-sent messages this is the admin id and {@link #fromAdmin} is true. */
    private String senderId;

    /** Cached display name at send time, so history renders without extra lookups. */
    private String senderName;

    /** True when an admin posted this (rendered/badged differently). */
    private boolean fromAdmin;

    private ChatMessageType type;

    /** Text body (caption for media messages; may be null/blank for pure media). */
    private String text;

    /** Cloudinary URL for IMAGE / FILE / VOICE messages. */
    private String mediaUrl;

    /** Original file name for FILE attachments (shown in the bubble). */
    private String mediaName;

    /** MIME type of the attached media, if any. */
    private String mediaMimeType;

    /** Soft-delete flag set by admin moderation; hidden from clients when true. */
    private boolean deleted;

    private Instant createdAt;

    public ChatMessage() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public boolean isFromAdmin() {
        return fromAdmin;
    }

    public void setFromAdmin(boolean fromAdmin) {
        this.fromAdmin = fromAdmin;
    }

    public ChatMessageType getType() {
        return type;
    }

    public void setType(ChatMessageType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getMediaMimeType() {
        return mediaMimeType;
    }

    public void setMediaMimeType(String mediaMimeType) {
        this.mediaMimeType = mediaMimeType;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
