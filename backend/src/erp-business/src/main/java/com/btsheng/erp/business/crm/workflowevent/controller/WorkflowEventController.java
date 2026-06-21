package com.btsheng.erp.business.crm.workflowevent.controller;

import com.btsheng.erp.business.crm.workflowevent.dto.WorkflowEventStatsDTO;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * V1.3.8 Sprint 10 Story 10.3 · 审批事件 Controller
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@RestController
@RequestMapping("/workflow/events")
@Tag(name = "V1.3.8-Sprint10-审批事件")
public class WorkflowEventController {

    private final WorkflowEventService service;

    @Autowired
    public WorkflowEventController(WorkflowEventService service) {
        this.service = service;
    }

    /**
     * V1.3.8 Sprint 10 Story 10.3 · 统计端点
     *
     * <p>权限：仅 GM + ADMIN
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('GM', 'ADMIN')")
    @Operation(summary = "审批事件统计（Story 10.3）")
    public Result<WorkflowEventStatsDTO> stats(
            @RequestParam("workflow_code") String workflowCode,
            @RequestParam(value = "approver_role", required = false) String approverRole,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.stats(workflowCode, approverRole, startDate, endDate);
    }
}