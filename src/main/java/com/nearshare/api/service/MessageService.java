package com.nearshare.api.service;

import com.nearshare.api.model.Message;
import com.nearshare.api.model.User;
import com.nearshare.api.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<User> conversations(User current) {
        return messageRepository.findBySenderOrReceiverAndNotDeleted(current).stream().map(m -> m.getSender().getId().equals(current.getId()) ? m.getReceiver() : m.getSender()).distinct().toList();
    }

    public List<Message> history(User current, User other) {
        return messageRepository.conversationForUser(current, other);
    }

    public Message send(User sender, User receiver, String content, String imageUrl) {
        Message m = Message.builder().id(UUID.randomUUID()).content(content).imageUrl(imageUrl).timestamp(LocalDateTime.now()).isRead(false).sender(sender).receiver(receiver).build();
        return messageRepository.save(m);
    }

    public List<Message> inbox(User current) {
        return messageRepository.findInboxForUser(current);
    }

    public List<Message> outbox(User current) {
        return messageRepository.findOutboxForUser(current);
    }

    public void delete(User current, UUID id) {
        Message m = messageRepository.findById(id).orElseThrow(() -> new RuntimeException("message_not_found"));
        if (!m.getSender().getId().equals(current.getId()) && !m.getReceiver().getId().equals(current.getId())) throw new RuntimeException("forbidden");
        
        if (m.getSender().getId().equals(current.getId())) {
            m.setDeletedBySender(true);
        }
        if (m.getReceiver().getId().equals(current.getId())) {
            m.setDeletedByReceiver(true);
        }
        
        if (m.isDeletedBySender() && m.isDeletedByReceiver()) {
            messageRepository.deleteById(id);
        } else {
            messageRepository.save(m);
        }
    }
}