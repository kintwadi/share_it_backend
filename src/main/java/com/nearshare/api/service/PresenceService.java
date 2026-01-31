package com.nearshare.api.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private final ConcurrentHashMap<String, UUID> sessionToUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> userSessionCounts = new ConcurrentHashMap<>();

    public void online(String sessionId, UUID userId) {
        sessionToUser.put(sessionId, userId);
        userSessionCounts.merge(userId, 1, Integer::sum);
    }

    public UUID offline(String sessionId) {
        UUID userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            userSessionCounts.computeIfPresent(userId, (k, v) -> v > 1 ? v - 1 : null);
        }
        return userId;
    }

    public boolean isOnline(UUID userId) {
        Integer c = userSessionCounts.get(userId);
        return c != null && c > 0;
    }

    public List<UUID> getOnlineUserIds() {
        return userSessionCounts.keySet().stream().filter(id -> {
            Integer c = userSessionCounts.get(id);
            return c != null && c > 0;
        }).toList();
    }
}