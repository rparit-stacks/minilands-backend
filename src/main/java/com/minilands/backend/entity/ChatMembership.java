package com.minilands.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Per-(property, user) chat membership override. Membership itself is implied
 * by an ACTIVE holding in the property; this document only records admin
 * moderation state (muted / removed). Absence of a document = normal member.
 */
@Document(collection = "chat_memberships")
@CompoundIndex(name = "property_user_idx", def = "{'propertyId': 1, 'userId': 1}", unique = true)
public class ChatMembership {

    @Id
    private String id;

    private String propertyId;
    private String userId;

    /** Muted users can read but not send. */
    private boolean muted;

    /** Removed users can neither read nor send (kicked from the group). */
    private boolean removed;

    private Instant updatedAt;

    public ChatMembership() {
    }

    public ChatMembership(String propertyId, String userId) {
        this.propertyId = propertyId;
        this.userId = userId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
