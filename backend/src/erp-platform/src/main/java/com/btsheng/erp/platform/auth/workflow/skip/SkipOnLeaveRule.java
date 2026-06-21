package com.btsheng.erp.platform.auth.workflow.skip;

import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.platform.auth.service.UserAvailabilityService;
import com.btsheng.erp.platform.auth.workflow.enums.SkipReason;
import com.btsheng.erp.platform.auth.workflow.mapper.ApprovalRecordMapper;
import com.btsheng.erp.platform.auth.workflow.entity.ApprovalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 跳过请假用户规则（V1.3.7 · Story 1.2 · T1.5 · P1 修补 ②）
 *
 * <p>读取 platform 本地 {@code sys_user.availability_status}（由 erp-business HR 模块 Feign 同步）。
 * 若 {@code ON_LEAVE / ON_TRIP / DISABLED / RESIGNED} → 标记候选人 {@code SKIPPED}。
 *
 * <p>查询失败 → 降级为"全员不跳过"（V1.3.7 红线：宁可误审不可漏审）。
 */
@Component
public class SkipOnLeaveRule {

    private static final Logger log = LoggerFactory.getLogger(SkipOnLeaveRule.class);

    /** 需要跳过的状态集合 */
    private static final Set<String> SKIP_STATUS = Set.of(
            "ON_LEAVE", "ON_TRIP", "DISABLED", "RESIGNED");

    private final UserAvailabilityService userAvailabilityService;
    private final ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    public SkipOnLeaveRule(UserAvailabilityService userAvailabilityService,
                           ApprovalRecordMapper approvalRecordMapper) {
        this.userAvailabilityService = userAvailabilityService;
        this.approvalRecordMapper = approvalRecordMapper;
    }

    @AuditLog(module = "approval", action = "approval.skip_on_leave")
    public SkipResult evaluate(Long approvalId, List<Long> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new SkipResult(false, candidates == null ? new ArrayList<>() : candidates);
        }
        List<Long> activeCandidates = new ArrayList<>();
        List<SkippedUser> skipped = new ArrayList<>();

        for (Long userId : candidates) {
            try {
                Optional<UserAvailabilityService.Availability> availability =
                        userAvailabilityService.findByUserId(userId);
                if (availability.isEmpty()) {
                    log.warn("[SkipOnLeaveRule] user availability empty for userId={}, keep ACTIVE", userId);
                    activeCandidates.add(userId);
                    continue;
                }
                String status = availability.get().effectiveSkipStatus();
                if (SKIP_STATUS.contains(status)) {
                    SkipReason reason = SkipReason.fromCode(status);
                    skipped.add(new SkippedUser(userId, reason == null ? SkipReason.ON_LEAVE : reason,
                            availability.get().getLeaveNo()));
                    if (log.isDebugEnabled()) {
                        log.debug("[SkipOnLeaveRule] userId={} → SKIPPED reason={} leaveNo={}",
                                userId, status, availability.get().getLeaveNo());
                    }
                } else {
                    activeCandidates.add(userId);
                }
            } catch (Exception e) {
                log.error("[SkipOnLeaveRule-DEGRADED] availability lookup failed for userId={}, keep ACTIVE: {}",
                        userId, e.getMessage());
                activeCandidates.add(userId);
            }
        }

        if (approvalId != null && !skipped.isEmpty()) {
            for (SkippedUser s : skipped) {
                ApprovalRecord rec = approvalRecordMapper.selectById(approvalId);
                if (rec != null) {
                    rec.setStatus("SKIPPED");
                    rec.setSkipReason(s.reason.getCode());
                    rec.setSkippedAt(LocalDateTime.now());
                    approvalRecordMapper.updateById(rec);
                }
            }
        }

        boolean allSkipped = activeCandidates.isEmpty() && !candidates.isEmpty();
        return new SkipResult(allSkipped, activeCandidates, skipped);
    }

    public static class SkipResult {
        private final boolean nodeSkipped;
        private final List<Long> activeCandidates;
        private final List<SkippedUser> skippedUsers;

        public SkipResult(boolean nodeSkipped, List<Long> activeCandidates) {
            this(nodeSkipped, activeCandidates, new ArrayList<>());
        }

        public SkipResult(boolean nodeSkipped, List<Long> activeCandidates, List<SkippedUser> skippedUsers) {
            this.nodeSkipped = nodeSkipped;
            this.activeCandidates = activeCandidates;
            this.skippedUsers = skippedUsers;
        }

        public boolean isNodeSkipped() { return nodeSkipped; }
        public List<Long> getActiveCandidates() { return activeCandidates; }
        public List<SkippedUser> getSkippedUsers() { return skippedUsers; }
    }

    public static class SkippedUser {
        private final Long userId;
        private final SkipReason reason;
        private final String hrLeaveNo;

        public SkippedUser(Long userId, SkipReason reason, String hrLeaveNo) {
            this.userId = userId;
            this.reason = reason;
            this.hrLeaveNo = hrLeaveNo;
        }

        public Long getUserId() { return userId; }
        public SkipReason getReason() { return reason; }
        public String getHrLeaveNo() { return hrLeaveNo; }
    }
}
