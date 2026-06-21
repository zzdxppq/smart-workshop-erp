package com.btsheng.erp.platform.auth.workflow.service;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.dto.*;
import com.btsheng.erp.platform.auth.workflow.entity.ApprovalRecord;

/**
 * 审批 Service 接口（V1.3.7 · Story 1.2 · T2.2）
 */
public interface ApprovalService {

    Result<ApprovalVO> createApproval(ApprovalCreateRequest req);

    Result<PageResponse<ApprovalVO>> getPendingApprovals(Long approverUserId, int pageNum, int pageSize);

    Result<PageResponse<ApprovalVO>> getMyPendingApprovals(Long applicantUserId, int pageNum, int pageSize);

    Result<ApprovalVO> approve(Long approvalId, ApproveRequest req);

    Result<ApprovalVO> reject(Long approvalId, RejectRequest req);

    Result<Void> urge(Long approvalId, Long operatorUserId);

    Result<ApprovalRecord> getApproval(Long id);
}
