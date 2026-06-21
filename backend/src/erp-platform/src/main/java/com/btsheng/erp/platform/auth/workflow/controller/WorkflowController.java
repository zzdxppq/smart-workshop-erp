package com.btsheng.erp.platform.auth.workflow.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.dto.*;
import com.btsheng.erp.platform.auth.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流 Controller（V1.3.7 · Story 1.2 · T3.1）
 *
 * <p>6 端点：POST/GET/PUT/DELETE + GET/{id} + POST /{id}/test。
 */
@Tag(name = "E1-Workflow", description = "审批工作流配置")
@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Autowired
    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Operation(summary = "创建工作流")
    @PostMapping
    @PreAuthorize("hasAuthority('workflow:create')")
    public Result<WorkflowVO> create(@RequestBody WorkflowCreateRequest req) {
        return workflowService.createWorkflow(req);
    }

    @Operation(summary = "工作流列表（分页）")
    @GetMapping
    @PreAuthorize("hasAuthority('workflow:read')")
    public Result<PageResponse<WorkflowVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return workflowService.listWorkflows(pageNum, pageSize, status, keyword);
    }

    @Operation(summary = "工作流详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:read')")
    public Result<WorkflowVO> get(@PathVariable Long id) {
        return workflowService.getWorkflow(id);
    }

    @Operation(summary = "修改工作流")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:update')")
    public Result<WorkflowVO> update(@PathVariable Long id, @RequestBody WorkflowUpdateRequest req) {
        return workflowService.updateWorkflow(id, req);
    }

    @Operation(summary = "试跑工作流（不落库）")
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('workflow:read')")
    public Result<WorkflowTestResult> test(@PathVariable Long id, @RequestBody WorkflowTestRequest req) {
        return workflowService.testWorkflow(id, req);
    }

    @Operation(summary = "删除工作流（内置模板 40904）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        return workflowService.deleteWorkflow(id);
    }
}
