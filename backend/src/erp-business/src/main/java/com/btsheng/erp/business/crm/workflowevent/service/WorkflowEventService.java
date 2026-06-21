package com.btsheng.erp.business.crm.workflowevent.service;

import com.btsheng.erp.business.crm.workflowevent.dto.WorkflowEventStatsDTO;
import com.btsheng.erp.business.crm.workflowevent.entity.SysWorkflowEvent;
import com.btsheng.erp.business.crm.workflowevent.mapper.SysWorkflowEventMapper;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * V1.3.8 Sprint 8 Story 8.3 · 审批事件 Service
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #recordEvent} 记录审批事件</li>
 * </ul>
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@Service
public class WorkflowEventService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventService.class);

    public static final String EVENT_CREATED = "CREATED";
    public static final String EVENT_APPROVED = "APPROVED";
    public static final String EVENT_REJECTED = "REJECTED";
    public static final String EVENT_DELEGATED = "DELEGATED";

    private final SysWorkflowEventMapper mapper;

    @Autowired
    public WorkflowEventService(SysWorkflowEventMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * AC-8.3.1：记录审批事件
     * <p>approval complete / approve / reject / delegated 时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<SysWorkflowEvent> recordEvent(
            String workflowCode,
            Long bizId,
            String bizNo,
            String eventType,
            String approverRole,
            Long approverUserId,
            String approverUserName,
            String comment,
            Integer matchedNodeIndex,
            String matchedThreshold) {

        SysWorkflowEvent event = new SysWorkflowEvent();
        event.setEventNo(generateEventNo());
        event.setWorkflowCode(workflowCode);
        event.setBizId(bizId);
        event.setBizNo(bizNo);
        event.setEventType(eventType);
        event.setApproverRole(approverRole);
        event.setApproverUserId(approverUserId);
        event.setApproverUserName(approverUserName);
        event.setComment(comment);
        event.setMatchedNodeIndex(matchedNodeIndex == null ? null : matchedNodeIndex);
        event.setMatchedThreshold(matchedThreshold);
        event.setCreatedAt(LocalDateTime.now());

        mapper.insert(event);

        log.info("[WorkflowEventService] recordEvent ok: eventNo={} workflow={} bizId={} type={} role={}",
                event.getEventNo(), workflowCode, bizId, eventType, approverRole);

        return Result.ok(event);
    }

    /**
     * V1.3.8 Sprint 10 Story 10.3 · AC-10.3.1/2 聚合统计
     *
     * <p>返回按 event_type + approver_role 分组的统计
     */
    public Result<WorkflowEventStatsDTO> stats(String workflowCode, String approverRole,
                                                 LocalDate startDate, LocalDate endDate) {
        // V1.3.8 Sprint 10 Story 10.3 · 入参校验
            if (workflowCode == null || workflowCode.isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "workflowCode 必填");
        }
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        // BR-10.3.1 start_date ≤ end_date 边界
            if (start.isAfter(end)) {
            return Result.fail(Result.CODE_PARAM_MISSING, "start_date 必须 ≤ end_date");
        }
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();

        Long total = mapper.countByWorkflowCode(workflowCode, approverRole, startDt, endDt);
        if (total == null) total = 0L;

        Map<String, Long> byEventType = new HashMap<>();
        for (Map<String, Object> row : mapper.aggregateByEventType(workflowCode, approverRole, startDt, endDt)) {
            byEventType.put((String) row.get("eventType"), ((Number) row.get("cnt")).longValue());
        }

        Map<String, Long> byApproverRole = new HashMap<>();
        for (Map<String, Object> row : mapper.aggregateByApproverRole(workflowCode, approverRole, startDt, endDt)) {
            byApproverRole.put((String) row.get("approverRole"), ((Number) row.get("cnt")).longValue());
        }

        WorkflowEventStatsDTO dto = new WorkflowEventStatsDTO();
        dto.setTotalCount(total);
        dto.setByEventType(byEventType);
        dto.setByApproverRole(byApproverRole);
        WorkflowEventStatsDTO.Period period = new WorkflowEventStatsDTO.Period();
        period.setStartDate(start);
        period.setEndDate(end);
        dto.setPeriod(period);

        log.info("[WorkflowEventService] stats ok: workflow={} approverRole={} total={} range=[{},{}]",
                workflowCode, approverRole, total, start, end);

        return Result.ok(dto);
    }

    /**
     * 生成 event_no：EV-{yyyyMMddHHmmss}-{UUID 前 4 位}
     */
    private String generateEventNo() {
        String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "EV-" + ts + "-" + uuid;
    }
}