package com.btsheng.erp.platform.auth.workflow.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.dto.ApprovalVO;
import com.btsheng.erp.platform.auth.workflow.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的审批 Controller（V1.3.7 · Story 1.2 · T3.4）
 *
 * <p>端点：GET /approvals/my-pending - 我作为申请人发起的待审批单。
 * 与 /approvals/pending 的区别：本端点是申请人维度，彼端点是审批人维度。
 */
@Tag(name = "E1-Workflow", description = "我的审批（申请人视角）")
@RestController
@RequestMapping("/approvals")
public class MyApprovalController {

    private final ApprovalService approvalService;

    @Autowired
    public MyApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Operation(summary = "我的待办（作为申请人，看自己提交的单子进度）")
    @GetMapping("/my-pending")
    @PreAuthorize("hasAuthority('approval:read')")
    public Result<PageResponse<ApprovalVO>> myPending(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam Long applicantUserId) {
        return approvalService.getMyPendingApprovals(applicantUserId, pageNum, pageSize);
    }
}
