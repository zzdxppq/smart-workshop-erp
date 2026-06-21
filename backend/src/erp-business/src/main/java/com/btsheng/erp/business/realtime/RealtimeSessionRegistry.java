package com.btsheng.erp.business.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/** WS 频道订阅管理（Spec B.2 · 9 频道） */
@Component
public class RealtimeSessionRegistry {

    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();

    public void subscribe(String channel, WebSocketSession session) {
        channelSessions.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unsubscribe(WebSocketSession session) {
        channelSessions.values().forEach(set -> set.remove(session));
    }

    public void broadcast(String channel, String jsonPayload) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage msg = new TextMessage(jsonPayload);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (IOException ignored) {
                    sessions.remove(s);
                }
            } else {
                sessions.remove(s);
            }
        }
    }
}
