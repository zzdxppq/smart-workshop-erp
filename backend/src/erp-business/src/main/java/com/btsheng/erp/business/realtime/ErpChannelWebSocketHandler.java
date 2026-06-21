package com.btsheng.erp.business.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;

/** 原生 WebSocket · ?channel=dashboard:kpi（Spec B.2） */
@Component
public class ErpChannelWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ErpChannelWebSocketHandler(RealtimeSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String channel = parseChannel(session.getUri());
        if (channel == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        registry.subscribe(channel, session);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                Map.of("type", "connected", "channel", channel))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unsubscribe(session);
    }

    private String parseChannel(URI uri) {
        if (uri == null || uri.getQuery() == null) return null;
        for (String part : uri.getQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "channel".equals(kv[0])) {
                return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
