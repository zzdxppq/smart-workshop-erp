package com.btsheng.erp.platform.audit.task;

import com.btsheng.erp.platform.audit.mapper.AuditLogArchiveMapper;
import com.btsheng.erp.platform.audit.mapper.AuditLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.3 · AC-1.3.5 · AuditCleanupTask 测例（3 测例 · P1 修补 ⑤） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditCleanupTaskTest {

    @Mock private AuditLogMapper auditLogMapper;
    @Mock private AuditLogArchiveMapper archiveMapper;

    @Test void archive_1_year_old() {
        when(auditLogMapper.archiveOldData(365)).thenReturn(2);
        AuditCleanupTask task = new AuditCleanupTask(auditLogMapper, archiveMapper);
        // 手动触发
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(task, "execute");
        verify(auditLogMapper, atLeastOnce()).archiveOldData(365);
    }

    @Test void keep_recent_1_year() {
        when(auditLogMapper.archiveOldData(365)).thenReturn(0);
        AuditCleanupTask task = new AuditCleanupTask(auditLogMapper, archiveMapper);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(task, "execute");
        verify(auditLogMapper, atLeastOnce()).archiveOldData(365);
    }

    @Test void zone_shanghai() throws Exception {
        // 验证 cron 表达式 + zone 字段
            java.lang.reflect.Method m = AuditCleanupTask.class.getDeclaredMethod("execute");
        org.springframework.scheduling.annotation.Scheduled s = m.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        assertNotNull(s);
        assertEquals("0 0 3 * * ?", s.cron());
        assertEquals("Asia/Shanghai", s.zone());
    }
}
