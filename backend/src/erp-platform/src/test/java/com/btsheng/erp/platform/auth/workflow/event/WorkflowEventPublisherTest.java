package com.btsheng.erp.platform.auth.workflow.event;

import com.btsheng.erp.core.integration.IntegrationEventPublisher;
import com.btsheng.erp.core.redis.RedisStreamTemplate;
import com.btsheng.erp.platform.auth.workflow.config.WorkflowConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowEventPublisherTest {

    @Mock private RedisStreamTemplate redisStream;
    @Mock private WorkflowConfig config;
    @Mock private IntegrationEventPublisher integrationEventPublisher;

    private WorkflowEventPublisher publisher() {
        return new WorkflowEventPublisher(redisStream, config, integrationEventPublisher);
    }

    @Test void publish_created_no_exception() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishCreated(1L, List.of(10010L, 10011L), "QUOTE", "BJ001", new BigDecimal("60000")));
    }

    @Test void publish_4_channels_no_exception() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM", "EMAIL", "APP_PUSH", "WECHAT_WORK"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishCreated(1L, List.of(10010L), "QUOTE", "BJ002", new BigDecimal("50000")));
    }

    @Test void publish_approved_no_exception() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishApproved(1L, 10010L, 2, "QUOTE", "BJ001"));
    }

    @Test void publish_overdue_no_exception() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishOverdue(1L, 25.0));
    }

    @Test void publish_rejected_with_reason() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishRejected(1L, 10010L, "价格过高", "QUOTE", "BJ001"));
    }
}
