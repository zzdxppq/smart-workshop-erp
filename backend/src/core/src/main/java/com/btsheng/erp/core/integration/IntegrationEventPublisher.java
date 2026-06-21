package com.btsheng.erp.core.integration;

import com.btsheng.erp.core.redis.RedisStreamTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** 跨服务集成事件发布（Redis Stream · 替代同步 Feign 推送类场景） */
@Component
public class IntegrationEventPublisher {

    private final RedisStreamTemplate redisStreamTemplate;

    public IntegrationEventPublisher(RedisStreamTemplate redisStreamTemplate) {
        this.redisStreamTemplate = redisStreamTemplate;
    }

    public void publish(String eventType, Map<String, String> payload) {
        Map<String, String> body = new HashMap<>(payload);
        body.put("eventType", eventType);
        body.put("ts", String.valueOf(System.currentTimeMillis()));
        redisStreamTemplate.publish(IntegrationStreams.STREAM_INTEGRATION, body);
    }
}
