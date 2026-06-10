package com.minilands.backend.service.chat;

import com.minilands.backend.dto.chat.ChatMemberResponse;
import com.minilands.backend.dto.chat.ChatMessagePage;
import com.minilands.backend.dto.chat.ChatMessageResponse;
import com.minilands.backend.dto.chat.SendChatMessageRequest;
import com.minilands.backend.entity.ChatMembership;
import com.minilands.backend.entity.ChatMessage;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.ChatMessageType;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.repository.ChatMembershipRepository;
import com.minilands.backend.repository.ChatMessageRepository;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-property group chat. The "group" is implicit: every user with an ACTIVE
 * holding in the property is a member, plus admins. Membership overrides
 * (mute / remove) live in {@link ChatMembership}.
 *
 * Messages are persisted and then broadcast over STOMP to
 * {@code /topic/property.<propertyId>}.
 */
@Service
public class ChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository messageRepository;
    private final ChatMembershipRepository membershipRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ChatMessageRepository messageRepository,
            ChatMembershipRepository membershipRepository,
            PropertyHoldingRepository holdingRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
        this.holdingRepository = holdingRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public static String topicFor(String propertyId) {
        return "/topic/property." + propertyId;
    }

    // ── Membership ───────────────────────────────────────────────────────────

    /** True if the user holds an ACTIVE position in the property. */
    public boolean isInvestor(String propertyId, String userId) {
        return holdingRepository.findByUserIdAndPropertyId(userId, propertyId)
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .isPresent();
    }

    /** Investor can read the chat: must be an investor and not removed. */
    public void assertCanRead(String propertyId, String userId) {
        requireProperty(propertyId);
        if (!isInvestor(propertyId, userId)) {
            throw new IllegalArgumentException("You must own a share in this property to view its chat.");
        }
        if (isRemoved(propertyId, userId)) {
            throw new IllegalArgumentException("You have been removed from this chat.");
        }
    }

    /** Investor can post: can read AND not muted. */
    public void assertCanSend(String propertyId, String userId) {
        assertCanRead(propertyId, userId);
        if (isMuted(propertyId, userId)) {
            throw new IllegalArgumentException("You have been muted in this chat.");
        }
    }

    private boolean isMuted(String propertyId, String userId) {
        return membershipRepository.findByPropertyIdAndUserId(propertyId, userId)
                .map(ChatMembership::isMuted)
                .orElse(false);
    }

    private boolean isRemoved(String propertyId, String userId) {
        return membershipRepository.findByPropertyIdAndUserId(propertyId, userId)
                .map(ChatMembership::isRemoved)
                .orElse(false);
    }

    // ── Sending ────────────────────────────────────────────────────────────────

    /** Investor sends a message (already membership-checked by the caller path). */
    @Transactional
    public ChatMessageResponse sendAsInvestor(String propertyId, String userId, SendChatMessageRequest request) {
        assertCanSend(propertyId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return persistAndBroadcast(propertyId, userId, displayName(user), false, request);
    }

    /**
     * Posts a "X joined the group" system line when a user first invests in a
     * property. Best-effort — never blocks the investment that triggered it.
     */
    @Transactional
    public void postJoinNotice(String propertyId, String userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return;
            }
            ChatMessage msg = new ChatMessage();
            msg.setPropertyId(propertyId);
            msg.setSenderId(userId);
            msg.setSenderName(displayName(user));
            msg.setFromAdmin(false);
            msg.setType(ChatMessageType.SYSTEM);
            msg.setText(displayName(user) + " joined the group");
            msg.setDeleted(false);
            msg.setCreatedAt(Instant.now());
            ChatMessage saved = messageRepository.save(msg);
            messagingTemplate.convertAndSend(topicFor(propertyId), toResponse(saved));
        } catch (Exception ignored) {
            // System join notice is non-critical.
        }
    }

    /** Admin posts a message into a property's group. */
    @Transactional
    public ChatMessageResponse sendAsAdmin(String propertyId, String adminId, String adminName, SendChatMessageRequest request) {
        requireProperty(propertyId);
        return persistAndBroadcast(propertyId, adminId, adminName != null ? adminName : "Admin", true, request);
    }

    private ChatMessageResponse persistAndBroadcast(
            String propertyId,
            String senderId,
            String senderName,
            boolean fromAdmin,
            SendChatMessageRequest request) {
        validatePayload(request);

        ChatMessage msg = new ChatMessage();
        msg.setPropertyId(propertyId);
        msg.setSenderId(senderId);
        msg.setSenderName(senderName);
        msg.setFromAdmin(fromAdmin);
        msg.setType(request.type());
        msg.setText(trimToNull(request.text()));
        msg.setMediaUrl(trimToNull(request.mediaUrl()));
        msg.setMediaName(trimToNull(request.mediaName()));
        msg.setMediaMimeType(trimToNull(request.mediaMimeType()));
        msg.setDeleted(false);
        msg.setCreatedAt(Instant.now());
        ChatMessage saved = messageRepository.save(msg);

        ChatMessageResponse response = toResponse(saved);
        messagingTemplate.convertAndSend(topicFor(propertyId), response);
        return response;
    }

    private void validatePayload(SendChatMessageRequest request) {
        if (request.type() == ChatMessageType.TEXT) {
            if (trimToNull(request.text()) == null) {
                throw new IllegalArgumentException("Text message cannot be empty.");
            }
        } else if (request.type() == ChatMessageType.IMAGE
                || request.type() == ChatMessageType.FILE
                || request.type() == ChatMessageType.VOICE) {
            if (trimToNull(request.mediaUrl()) == null) {
                throw new IllegalArgumentException("Media message requires a mediaUrl.");
            }
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + request.type());
        }
    }

    // ── History ─────────────────────────────────────────────────────────────

    /** Investor history (excludes deleted messages). */
    public ChatMessagePage getHistoryForInvestor(String propertyId, String userId, int page, int size) {
        assertCanRead(propertyId, userId);
        Page<ChatMessage> result = messageRepository
                .findByPropertyIdAndDeletedFalseOrderByCreatedAtDesc(propertyId, pageable(page, size));
        return toPage(result);
    }

    /** Admin history (includes deleted, for moderation/audit). */
    public ChatMessagePage getHistoryForAdmin(String propertyId, int page, int size) {
        requireProperty(propertyId);
        Page<ChatMessage> result = messageRepository
                .findByPropertyIdOrderByCreatedAtDesc(propertyId, pageable(page, size));
        return toPage(result);
    }

    // ── Admin moderation ─────────────────────────────────────────────────────

    /** Soft-delete a message and broadcast the redaction so clients drop it live. */
    @Transactional
    public void deleteMessage(String messageId) {
        ChatMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        msg.setDeleted(true);
        messageRepository.save(msg);
        // Broadcast the now-deleted message so live clients can remove it.
        messagingTemplate.convertAndSend(topicFor(msg.getPropertyId()), toResponse(msg));
    }

    @Transactional
    public void setMuted(String propertyId, String userId, boolean muted) {
        ChatMembership m = membershipRepository.findByPropertyIdAndUserId(propertyId, userId)
                .orElseGet(() -> new ChatMembership(propertyId, userId));
        m.setMuted(muted);
        m.setUpdatedAt(Instant.now());
        membershipRepository.save(m);
    }

    @Transactional
    public void setRemoved(String propertyId, String userId, boolean removed) {
        ChatMembership m = membershipRepository.findByPropertyIdAndUserId(propertyId, userId)
                .orElseGet(() -> new ChatMembership(propertyId, userId));
        m.setRemoved(removed);
        m.setUpdatedAt(Instant.now());
        membershipRepository.save(m);
    }

    /** Members of a property's chat (ACTIVE investors) with their moderation flags. */
    public List<ChatMemberResponse> getMembers(String propertyId) {
        requireProperty(propertyId);

        // Moderation overrides keyed by userId.
        Map<String, ChatMembership> overrides = new LinkedHashMap<>();
        for (ChatMembership m : membershipRepository.findByPropertyId(propertyId)) {
            overrides.put(m.getUserId(), m);
        }

        List<PropertyHolding> holders =
                holdingRepository.findByPropertyIdAndStatus(propertyId, HoldingStatus.ACTIVE);

        // Distinct userIds (a user could in theory have multiple holding rows).
        Map<String, ChatMemberResponse> byUser = new LinkedHashMap<>();
        for (PropertyHolding h : holders) {
            String uid = h.getUserId();
            if (byUser.containsKey(uid)) {
                continue;
            }
            User user = userRepository.findById(uid).orElse(null);
            if (user == null) {
                continue;
            }
            ChatMembership o = overrides.get(uid);
            byUser.put(uid, new ChatMemberResponse(
                    uid,
                    displayName(user),
                    user.getEmail(),
                    o != null && o.isMuted(),
                    o != null && o.isRemoved()));
        }
        return List.copyOf(byUser.values());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Property requireProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }

    private ChatMessagePage toPage(Page<ChatMessage> result) {
        List<ChatMessageResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new ChatMessagePage(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getPropertyId(),
                m.getSenderId(),
                m.getSenderName(),
                m.isFromAdmin(),
                m.getType(),
                m.isDeleted() ? null : m.getText(),
                m.isDeleted() ? null : m.getMediaUrl(),
                m.isDeleted() ? null : m.getMediaName(),
                m.isDeleted() ? null : m.getMediaMimeType(),
                m.isDeleted(),
                m.getCreatedAt());
    }

    private String displayName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "Investor";
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
