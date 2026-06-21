package com.btsheng.erp.platform.audit.task;

import com.btsheng.erp.platform.audit.mapper.AuditLogArchiveMapper;
import com.btsheng.erp.platform.audit.mapper.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计清理任务（V1.3.7 Story 1.3 · AC-1.3.5 · P1 修补 ⑤）
 *
 * <p>每天凌晨 3 点（Asia/Shanghai）执行：1.5 年前数据归档到 sys_audit_log_archive + 主表删除
 * <p>保留期：主表 1 年 / 归档表 5 年（V1.3.7 §8 红线 5）
 */
@Component
public class AuditCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AuditCleanupTask.class);

    private final AuditLogMapper auditLogMapper;
    private final AuditLogArchiveMapper archiveMapper;

    public AuditCleanupTask(AuditLogMapper auditLogMapper, AuditLogArchiveMapper archiveMapper) {
        this.auditLogMapper = auditLogMapper;
        this.archiveMapper = archiveMapper;
    }

    /**
     * 每天凌晨 3 点（Asia/Shanghai）清理 1 年前数据
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void execute() {
        try {
            int archived = auditLogMapper.archiveOldData(365);
            log.info("[AuditCleanup] 归档完成: {} 条", archived);
        } catch (Exception e) {
            log.error("[AuditCleanup] 失败告警 → 飞书/邮件 → devops oncall", e);
            // 告警：飞书/邮件 → JIRA 链接待补
        }
    }
}
