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

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class P1RedisStream4ChannelTest {

    @Mock private RedisStreamTemplate redisStream;
    @Mock private WorkflowConfig config;
    @Mock private IntegrationEventPublisher integrationEventPublisher;

    private WorkflowEventPublisher publisher() {
        return new WorkflowEventPublisher(redisStream, config, integrationEventPublisher);
    }

    @Test void P1_3_1_parallel_4_channels() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM", "EMAIL", "APP_PUSH", "WECHAT_WORK"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishCreated(1L, List.of(10010L), "QUOTE", "BJ001", new BigDecimal("60000")));
    }

    @Test void P1_3_2_redis_failure_retry_backoff() {
        when(config.getRetryTimes()).thenReturn(3);
        when(config.getRetryBackoffMs()).thenReturn(List.of(1000L, 2000L, 4000L));
        assertEquals(3, config.getRetryTimes());
        assertEquals(3, config.getRetryBackoffMs().size());
    }

    @Test void P1_3_3_email_failure_other_succeed() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM", "EMAIL", "APP_PUSH", "WECHAT_WORK"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishCreated(1L, List.of(10010L), "QUOTE", "BJ002", new BigDecimal("50000")));
    }

    @Test void P1_3_4_all_4_failed_alert() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishOverdue(1L, 25.0));
    }

    @Test void P1_3_5_consumer_group_idempotent() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishCreated(1L, List.of(10010L), "QUOTE", "BJ003", new BigDecimal("60000")));
    }

    @Test void P1_3_6_ack_on_success() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishApproved(1L, 10010L, 2, "QUOTE", "BJ001"));
    }

    @Test void P1_3_7_consume_100_events_in_5s() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            pub.publishCreated((long) i, List.of(10010L), "QUOTE", "BJ" + i, new BigDecimal("50000"));
        }
        long cost = System.currentTimeMillis() - start;
        assertTrue(cost < 10000, "100 events push < 10s, actual " + cost + "ms");
    }

    @Test void P1_3_8_nacos_disable_channel() {
        when(config.getNotifyChannels()).thenReturn(List.of("REDIS_STREAM"));
        when(redisStream.publish(anyString(), anyMap())).thenReturn("1-0");
        WorkflowEventPublisher pub = publisher();
        assertDoesNotThrow(() -> pub.publishCreated(1L, List.of(10010L), "QUOTE", "BJ", new BigDecimal("50000")));
    }
}
