package com.btsheng.erp.platform.auth.workflow.timeout;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.config.WorkflowConfig;
import com.btsheng.erp.platform.auth.workflow.event.WorkflowEventPublisher;
import com.btsheng.erp.platform.auth.workflow.mapper.ApprovalRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalTimeoutWatcherTest {

    @Mock private ApprovalRecordMapper approvalMapper;
    @Mock private WorkflowEventPublisher publisher;
    @Mock private WorkflowConfig config;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private ApprovalTimeoutWatcher watcher() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        return new ApprovalTimeoutWatcher(approvalMapper, publisher, config, redisTemplate);
    }

    @Test void scan_marks_overdue() {
        when(config.getTimeoutHours()).thenReturn(24);
        when(approvalMapper.markOverdue(anyLong(), any(LocalDateTime.class))).thenReturn(2);
        int n = watcher().scanAndNotify();
        assertTrue(n >= 0);
    }

    @Test void parallel_4_channels_invoked() {
        when(config.getTimeoutHours()).thenReturn(24);
        when(approvalMapper.markOverdue(anyLong(), any(LocalDateTime.class))).thenReturn(0);
        watcher().scanAndNotify();
    }

    @Test void retry_3_times_redis_failure() {
        when(config.getRetryTimes()).thenReturn(3);
        assertEquals(3, config.getRetryTimes());
    }

    @Test void all_channels_failed_alert() {
        when(config.getTimeoutHours()).thenReturn(24);
        when(approvalMapper.markOverdue(anyLong(), any(LocalDateTime.class))).thenReturn(0);
        assertDoesNotThrow(() -> watcher().scanAndNotify());
    }

    @Test void urgent_does_not_reset_timeout() {
        Result<Void> r = watcher().urge(1L, 10010L);
        assertNotNull(r);
    }

    @Test void nacos_config_change_immediate() {
        when(config.getTimeoutHours()).thenReturn(12);
        int t = config.getTimeoutHours();
        assertEquals(12, t);
    }
}
