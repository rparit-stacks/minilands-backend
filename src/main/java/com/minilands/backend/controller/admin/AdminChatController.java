package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.auth.MessageResponse;
import com.minilands.backend.dto.chat.ChatMemberResponse;
import com.minilands.backend.dto.chat.ChatMessagePage;
import com.minilands.backend.dto.chat.ChatMessageResponse;
import com.minilands.backend.dto.chat.ModerationRequest;
import com.minilands.backend.dto.chat.SendChatMessageRequest;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin moderation + participation for per-property group chats:
 * view every message (incl. deleted), post announcements, delete messages,
 * mute / remove members, and list members.
 */
@RestController
@RequestMapping("/api/admin/chat/properties/{propertyId}")
public class AdminChatController {

    private final ChatService chatService;

    public AdminChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /// Full history including admin-deleted messages (for audit/moderation).
    @GetMapping("/messages")
    public ResponseEntity<ChatMessagePage> history(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getHistoryForAdmin(propertyId, page, size));
    }

    /// Post a message into the group as the admin (announcement).
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> post(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @Valid @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(
                chatService.sendAsAdmin(propertyId, principal.getAdminId(), "Admin", request));
    }

    /// Soft-delete a message; live clients see it disappear.
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> delete(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @PathVariable String messageId) {
        chatService.deleteMessage(messageId);
        return ResponseEntity.ok(new MessageResponse("Message deleted"));
    }

    /// Members of this property's chat with their moderation flags.
    @GetMapping("/members")
    public ResponseEntity<List<ChatMemberResponse>> members(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId) {
        return ResponseEntity.ok(chatService.getMembers(propertyId));
    }

    /// Mute / un-mute a member (muted = can read, can't send).
    @PutMapping("/members/{userId}/mute")
    public ResponseEntity<MessageResponse> mute(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @PathVariable String userId,
            @RequestBody ModerationRequest request) {
        chatService.setMuted(propertyId, userId, request.value());
        return ResponseEntity.ok(new MessageResponse(request.value() ? "User muted" : "User un-muted"));
    }

    /// Remove / re-admit a member (removed = can't read or send).
    @PutMapping("/members/{userId}/remove")
    public ResponseEntity<MessageResponse> remove(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String propertyId,
            @PathVariable String userId,
            @RequestBody ModerationRequest request) {
        chatService.setRemoved(propertyId, userId, request.value());
        return ResponseEntity.ok(new MessageResponse(request.value() ? "User removed" : "User re-admitted"));
    }
}
