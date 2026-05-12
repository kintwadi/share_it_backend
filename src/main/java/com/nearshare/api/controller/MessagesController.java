package com.nearshare.api.controller;

import com.nearshare.api.dto.MessageDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.model.User;
import com.nearshare.api.service.MessageService;
import com.nearshare.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {
    private final MessageService messageService;
    private final UserService userService;

    public MessagesController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<UserSummaryDTO>> conversations(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        List<UserSummaryDTO> list = messageService.conversations(current).stream()
                .map(u -> UserSummaryDTO.builder().id(u.getId()).name(u.getName()).trustScore(u.getTrustScore()).avatarUrl(u.getAvatarUrl()).build())
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/with/{userId}")
    public ResponseEntity<List<MessageDTO>> history(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("userId") UUID userId) {
        User current = userService.getByEmail(principal.getUsername());
        User other = userService.getById(userId);
        if (current.getId().equals(userId)) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(messageService.history(current, other).stream().map(m -> MessageDTO.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .receiverId(m.getReceiver().getId())
                .senderEmail(m.getSender().getEmail())
                .receiverEmail(m.getReceiver().getEmail())
                .content(m.getContent())
                .imageUrl(m.getImageUrl())
                .timestamp(m.getTimestamp().toString())
                .isRead(m.isRead())
                .build()).toList());
    }

    @PostMapping("/")
    public ResponseEntity<MessageDTO> send(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody Map<String, String> payload) {
        User sender = userService.getByEmail(principal.getUsername());
        String receiverEmail = payload.get("receiverEmail");
        String receiverIdRaw = payload.get("receiverId");
        if ((receiverEmail == null || receiverEmail.isBlank()) && (receiverIdRaw == null || receiverIdRaw.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_receiver");
        }
        User receiver;
        if (receiverEmail != null && !receiverEmail.isBlank()) {
            receiver = userService.getByEmail(receiverEmail.trim());
        } else if (receiverIdRaw != null && receiverIdRaw.contains("@")) {
            receiver = userService.getByEmail(receiverIdRaw.trim());
        } else {
            UUID receiverId;
            try {
                receiverId = UUID.fromString(receiverIdRaw);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_receiver");
            }
            receiver = userService.getById(receiverId);
        }
        String content = payload.get("content");
        String imageUrl = payload.get("imageUrl");
        var m = messageService.send(sender, receiver, content, imageUrl);
        return ResponseEntity.ok(MessageDTO.builder()
                .id(m.getId())
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .senderEmail(sender.getEmail())
                .receiverEmail(receiver.getEmail())
                .content(m.getContent())
                .imageUrl(m.getImageUrl())
                .timestamp(m.getTimestamp().toString())
                .isRead(m.isRead())
                .build());
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageDTO>> inbox(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        var list = messageService.inbox(current).stream().map(m -> MessageDTO.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .receiverId(m.getReceiver().getId())
                .senderEmail(m.getSender().getEmail())
                .receiverEmail(m.getReceiver().getEmail())
                .content(m.getContent())
                .imageUrl(m.getImageUrl())
                .timestamp(m.getTimestamp().toString())
                .isRead(m.isRead())
                .build()).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<MessageDTO>> outbox(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        var list = messageService.outbox(current).stream().map(m -> MessageDTO.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .receiverId(m.getReceiver().getId())
                .senderEmail(m.getSender().getEmail())
                .receiverEmail(m.getReceiver().getEmail())
                .content(m.getContent())
                .imageUrl(m.getImageUrl())
                .timestamp(m.getTimestamp().toString())
                .isRead(m.isRead())
                .build()).toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User current = userService.getByEmail(principal.getUsername());
        messageService.delete(current, id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
