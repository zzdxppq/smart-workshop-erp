package com.btsheng.erp.platform.auth.workflow.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.dto.*;
import com.btsheng.erp.platform.auth.workflow.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 审批 Controller（V1.3.7 · Story 1.2 · T3.2）
 *
 * <p>4 端点：pending + approve + reject + urge。
 */
@Tag(name = "E1-Workflow", description = "审批工作流")
@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Autowired
    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Operation(summary = "我的待办（作为审批人）")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('approval:read')")
    public Result<PageResponse<ApprovalVO>> pending(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam Long approverUserId) {
        return approvalService.getPendingApprovals(approverUserId, pageNum, pageSize);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('approval:approve')")
    public Result<ApprovalVO> approve(@PathVariable Long id, @RequestBody ApproveRequest req) {
        return approvalService.approve(id, req);
    }

    @Operation(summary = "审批驳回")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('approval:reject')")
    public Result<ApprovalVO> reject(@PathVariable Long id, @RequestBody RejectRequest req) {
        return approvalService.reject(id, req);
    }

    @Operation(summary = "催办（V1.3.7 · 不重置 timeout_at）")
    @PostMapping("/{id}/urge")
    @PreAuthorize("hasAuthority('approval:read')")
    public Result<Void> urge(@PathVariable Long id, @RequestParam Long operatorUserId) {
        return approvalService.urge(id, operatorUserId);
    }
}
