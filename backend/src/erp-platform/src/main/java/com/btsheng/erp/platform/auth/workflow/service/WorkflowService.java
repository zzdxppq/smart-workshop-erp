package com.btsheng.erp.platform.auth.workflow.service;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.dto.*;

/**
 * 工作流 Service 接口（V1.3.7 · Story 1.2 · T2.1）
 */
public interface WorkflowService {

    Result<WorkflowVO> createWorkflow(WorkflowCreateRequest req);

    Result<WorkflowVO> updateWorkflow(Long id, WorkflowUpdateRequest req);

    Result<PageResponse<WorkflowVO>> listWorkflows(int pageNum, int pageSize, String status, String keyword);

    Result<WorkflowVO> getWorkflow(Long id);

    Result<WorkflowTestResult> testWorkflow(Long id, WorkflowTestRequest req);

    Result<Void> deleteWorkflow(Long id);
}
