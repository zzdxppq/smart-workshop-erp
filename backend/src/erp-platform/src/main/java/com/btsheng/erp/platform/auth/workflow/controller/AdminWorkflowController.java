package com.btsheng.erp.platform.auth.workflow.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.dto.WorkflowVO;
import com.btsheng.erp.platform.auth.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Web Workflows.vue 路径别名（/admin/workflows → /workflows） */
@Tag(name = "E1-Admin-Workflow", description = "管理工作流（Web 别名）")
@RestController
@RequestMapping("/admin/workflows")
public class AdminWorkflowController {

    private final WorkflowService workflowService;

    @Autowired
    public AdminWorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Operation(summary = "工作流列表（Web E1-S2）")
    @GetMapping
    public Result<PageResponse<WorkflowVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return workflowService.listWorkflows(pageNum, pageSize, status, keyword);
    }
}
