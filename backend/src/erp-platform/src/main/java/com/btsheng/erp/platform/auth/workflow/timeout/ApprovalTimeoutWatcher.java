package com.btsheng.erp.platform.auth.workflow.timeout;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.config.WorkflowConfig;
import com.btsheng.erp.platform.auth.workflow.entity.ApprovalRecord;
import com.btsheng.erp.platform.auth.workflow.event.WorkflowEventPublisher;
import com.btsheng.erp.platform.auth.workflow.mapper.ApprovalRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批超时扫描器（V1.3.7 · Story 1.2 · T1.3 · P1 修补 ③ · AC-1.2.4）
 *
 * <p>每 30 分钟扫一次（cron 由 XXL-JOB 触发）→ 4 通道并行推送 + 失败重试。
 *
 * <p>幂等保证：{@code FOR UPDATE SKIP LOCKED} 防止多实例并发扫描同一行。
 * 二次催办（{@code publishUrge}）不重置 {@code timeout_at}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class ApprovalTimeoutWatcher {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTimeoutWatcher.class);

    private final ApprovalRecordMapper approvalRecordMapper;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowConfig config;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public ApprovalTimeoutWatcher(ApprovalRecordMapper approvalRecordMapper,
                                  WorkflowEventPublisher eventPublisher,
                                  WorkflowConfig config,
                                  StringRedisTemplate redisTemplate) {
        this.approvalRecordMapper = approvalRecordMapper;
        this.eventPublisher = eventPublisher;
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 扫描 + 4 通道推送（分布式锁 SETNX 防并发）。
     */
    public int scanAndNotify() {
        String lockKey = config.getTimeoutScanLockKey();
        String lockToken = String.valueOf(System.currentTimeMillis());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(config.getTimeoutScanLockTtl()));
        if (acquired == null || !acquired) {
            log.info("[ApprovalTimeoutWatcher] 分布式锁已被占用，跳过本次扫描：key={}", lockKey);
            return 0;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<ApprovalRecord> overdue = approvalRecordMapper.findOverdue(now, 100);
            int count = 0;
            for (ApprovalRecord rec : overdue) {
                try {
                    int affected = approvalRecordMapper.markOverdue(rec.getId(), now);
                    if (affected > 0) {
                        double hours = rec.getTimeoutAt() == null ? config.getTimeoutHours()
                                : java.time.Duration.between(rec.getTimeoutAt(), now).toMinutes() / 60.0;
                        eventPublisher.publishOverdue(rec.getId(), hours);
                        count++;
                    }
                } catch (Exception e) {
                    log.error("[ApprovalTimeoutWatcher] 单条审批超时处理失败：id={} err={}",
                            rec.getId(), e.getMessage(), e);
                }
            }
            if (count > 0) {
                log.info("[ApprovalTimeoutWatcher] 本次扫描推送 {} 条超时审批", count);
            }
            return count;
        } finally {
            // 释放分布式锁（CAS：仅当 token 匹配时删除）
            String token = redisTemplate.opsForValue().get(lockKey);
            if (lockToken.equals(token)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 二次催办（不重置 timeout_at）。
     */
    public Result<Void> urge(Long approvalId, Long operatorUserId) {
        if (approvalId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "审批单 ID 必填");
        }
        ApprovalRecord rec = approvalRecordMapper.selectById(approvalId);
        if (rec == null) {
            return Result.fail(40401, "审批单不存在");
        }
        // V1.3.7 红线：不重置 timeout_at
            eventPublisher.publishUrge(approvalId, operatorUserId);
        return Result.ok("催办成功", null);
    }
}
