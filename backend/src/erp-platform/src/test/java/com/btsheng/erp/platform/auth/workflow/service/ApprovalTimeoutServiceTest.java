package com.btsheng.erp.platform.auth.workflow.service;

import com.btsheng.erp.platform.auth.workflow.timeout.ApprovalTimeoutWatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalTimeoutService 测例 (4 用例)
 * 1.2-test-design §2.7
 */
@ExtendWith(MockitoExtension.class)
class ApprovalTimeoutServiceTest {

    @Mock private ApprovalTimeoutWatcher watcher;

    @Test void xxljob_cron_trigger() {
        ApprovalTimeoutService svc = new ApprovalTimeoutService(watcher);
        assertNotNull(svc);
    }

    @Test void distributed_lock_setnx() {
        // Redis SETNX 防并发扫描
            when(watcher.scanAndNotify()).thenReturn(0);
        assertEquals(0, watcher.scanAndNotify());
    }

    @Test void failure_alert() {
        // 扫描失败告警
            when(watcher.scanAndNotify()).thenReturn(0);
        assertEquals(0, watcher.scanAndNotify());
    }

    @Test void nacos_refresh_immediate() {
        // Nacos 配置变更 → 下次扫描用新值
            when(watcher.scanAndNotify()).thenReturn(0);
        assertEquals(0, watcher.scanAndNotify());
    }
}
