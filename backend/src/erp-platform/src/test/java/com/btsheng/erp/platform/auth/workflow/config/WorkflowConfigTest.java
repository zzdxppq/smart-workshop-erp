package com.btsheng.erp.platform.auth.workflow.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowConfig 测例 (3 用例 · Nacos 热更新)
 * 1.2-test-design §2.7
 */
class WorkflowConfigTest {

    @Test void nacos_change_timeout_hours() {
        WorkflowConfig c = new WorkflowConfig();
        c.setTimeoutHours(12);
        assertEquals(12, c.getTimeoutHours());
    }

    @Test void nacos_change_scan_cron() {
        WorkflowConfig c = new WorkflowConfig();
        c.setOverdueScanCron("0 0 */1 * * *");
        assertEquals("0 0 */1 * * *", c.getOverdueScanCron());
    }

    @Test void refresh_scope_default_channels() {
        WorkflowConfig c = new WorkflowConfig();
        assertNotNull(c.getNotifyChannels());
        assertTrue(c.getNotifyChannels().contains("REDIS_STREAM"));
    }
}
