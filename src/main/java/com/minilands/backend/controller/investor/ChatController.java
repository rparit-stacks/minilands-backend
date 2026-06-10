package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.chat.ChatMessagePage;
import com.minilands.backend.dto.chat.ChatMessageResponse;
import com.minilands.backend.dto.chat.SendChatMessageRequest;
import com.minilands.backend.service.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Investor-facing per-property group chat.
 *
 * Sending and history go over REST (membership-checked here); live updates are
 * delivered to the client over STOMP on {@code /topic/property.<propertyId>}.
 */
@RestController
@RequestMapping("/api/chat/properties/{propertyId}")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /// Paginated message history (newest first). Excludes admin-deleted messages.
    @GetMapping("/messages")
    public ResponseEntity<ChatMessagePage> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
                chatService.getHistoryForInvestor(propertyId, principal.getUserId(), page, size));
    }

    /// Send a message. Media (image/file) is uploaded first via /api/media/upload,
    /// then its URL is passed here. The saved message is also broadcast over STOMP.
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> send(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String propertyId,
            @Valid @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(
                chatService.sendAsInvestor(propertyId, principal.getUserId(), request));
    }
}
