package com.btsheng.erp.business.crm.workflowevent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

/**
 * V1.3.8 Sprint 10 Story 10.3 · sys_workflow_event 统计报表 DTO
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@Data
@Schema(description = "审批事件统计（Story 10.3）")
public class WorkflowEventStatsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long totalCount;

    @Schema(description = "按事件类型分组计数")
    private Map<String, Long> byEventType;

    @Schema(description = "按审批角色分组计数")
    private Map<String, Long> byApproverRole;

    @Schema(description = "查询周期")
    private Period period;

    @Data
    public static class Period implements Serializable {
        private LocalDate startDate;
        private LocalDate endDate;
    }
}