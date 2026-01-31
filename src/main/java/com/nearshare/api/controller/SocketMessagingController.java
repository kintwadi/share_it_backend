package com.nearshare.api.controller;

import com.nearshare.api.dto.ChatMessageDTO;
import com.nearshare.api.dto.MessageDTO;
import com.nearshare.api.dto.PresenceUpdateDTO;
import com.nearshare.api.model.Message;
import com.nearshare.api.model.User;
import com.nearshare.api.service.MessageService;
import com.nearshare.api.service.PresenceService;
import com.nearshare.api.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class SocketMessagingController {
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final PresenceService presenceService;

    public SocketMessagingController(SimpMessagingTemplate messagingTemplate, MessageService messageService, UserService userService, PresenceService presenceService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userService = userService;
        this.presenceService = presenceService;
    }

    @MessageMapping("/chat.send")
    public void send(ChatMessageDTO dto) {
        if (dto.getSenderId() == null || dto.getReceiverId() == null) {
            return;
        }
        if ((dto.getContent() == null || dto.getContent().isBlank()) && (dto.getImageUrl() == null || dto.getImageUrl().isBlank())) {
            return;
        }
        User sender = userService.getById(dto.getSenderId());
        User receiver = userService.getById(dto.getReceiverId());
        Message m = Message.builder().id(UUID.randomUUID()).content(dto.getContent()).imageUrl(dto.getImageUrl()).timestamp(LocalDateTime.now()).isRead(false).sender(sender).receiver(receiver).build();
        m = messageService.send(sender, receiver, dto.getContent(), dto.getImageUrl());
        MessageDTO md = MessageDTO.builder().id(m.getId()).senderId(m.getSender().getId()).receiverId(m.getReceiver().getId()).content(m.getContent()).imageUrl(m.getImageUrl()).timestamp(m.getTimestamp().toString()).isRead(m.isRead()).build();
        messagingTemplate.convertAndSend("/topic/messages." + receiver.getId(), md);
        messagingTemplate.convertAndSend("/topic/messages." + sender.getId(), md);
    }

    @MessageMapping("/presence.online")
    public void presenceOnline(@Header("simpSessionId") String sessionId, String userIdStr) {
        if (sessionId == null || userIdStr == null || userIdStr.isBlank()) return;
        UUID userId = UUID.fromString(userIdStr);
        presenceService.online(sessionId, userId);
        PresenceUpdateDTO dto = PresenceUpdateDTO.builder().userId(userId).online(true).build();
        messagingTemplate.convertAndSend("/topic/presence", dto);
    }

    @EventListener(org.springframework.web.socket.messaging.SessionDisconnectEvent.class)
    public void onDisconnect(org.springframework.web.socket.messaging.SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId == null) return;
        UUID userId = presenceService.offline(sessionId);
        if (userId != null && !presenceService.isOnline(userId)) {
            PresenceUpdateDTO dto = PresenceUpdateDTO.builder().userId(userId).online(false).build();
            messagingTemplate.convertAndSend("/topic/presence", dto);
        }
    }
}