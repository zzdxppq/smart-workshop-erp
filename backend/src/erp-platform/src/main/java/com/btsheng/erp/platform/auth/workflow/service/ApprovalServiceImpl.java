package com.btsheng.erp.platform.auth.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.workflow.config.WorkflowConfig;
import com.btsheng.erp.platform.auth.workflow.dto.*;
import com.btsheng.erp.platform.auth.workflow.entity.ApprovalRecord;
import com.btsheng.erp.platform.auth.workflow.entity.Workflow;
import com.btsheng.erp.platform.auth.workflow.enums.ApprovalStatus;
import com.btsheng.erp.platform.auth.workflow.enums.BizType;
import com.btsheng.erp.platform.auth.workflow.event.WorkflowEventPublisher;
import com.btsheng.erp.platform.auth.workflow.mapper.ApprovalRecordMapper;
import com.btsheng.erp.platform.auth.workflow.mapper.WorkflowMapper;
import com.btsheng.erp.platform.auth.workflow.router.WorkflowApprovalRouter;
import com.btsheng.erp.platform.auth.workflow.skip.SkipOnLeaveRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批 Service 实装（V1.3.7 · Story 1.2 · T2.2）
 *
 * <p>OR 会签 + 状态机守卫 + {@code SkipOnLeaveRule} 集成。
 */
@Service
public class ApprovalServiceImpl implements ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalServiceImpl.class);

    private final WorkflowMapper workflowMapper;
    private final ApprovalRecordMapper approvalMapper;
    private final WorkflowApprovalRouter router;
    private final SkipOnLeaveRule skipOnLeaveRule;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ApprovalServiceImpl(WorkflowMapper workflowMapper,
                               ApprovalRecordMapper approvalMapper,
                               WorkflowApprovalRouter router,
                               SkipOnLeaveRule skipOnLeaveRule,
                               WorkflowEventPublisher eventPublisher,
                               WorkflowConfig config) {
        this.workflowMapper = workflowMapper;
        this.approvalMapper = approvalMapper;
        this.router = router;
        this.skipOnLeaveRule = skipOnLeaveRule;
        this.eventPublisher = eventPublisher;
        this.config = config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "approval", action = "approval.create")
    public Result<ApprovalVO> createApproval(ApprovalCreateRequest req) {
        if (req == null || req.getBizType() == null || req.getBizId() == null
                || req.getAmount() == null || req.getApplicantUserId() == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "bizType/bizId/amount/applicantUserId 必填");
        }
        if (req.getAmount().signum() <= 0) {
            return Result.fail(40007, "金额必须 > 0");
        }
        BizType bizType = BizType.fromCode(req.getBizType());
        if (bizType == null) {
            return Result.fail(40006, "bizType 非法：" + req.getBizType());
        }
        // 选 workflow_code（默认按 bizType 选）
            String workflowCode = req.getWorkflowCode();
        if (workflowCode == null) {
            workflowCode = bizType.getDefaultWorkflowCode();
        }
        Workflow wf = workflowMapper.findActiveByCode(workflowCode);
        if (wf == null) {
            return Result.fail(40008, "工作流 " + workflowCode + " 不存在或非 ACTIVE");
        }
        // 路由
            WorkflowApprovalRouter.RouteResult route =
                router.route(req.getAmount(), req.getApplicantUserId(), workflowCode);
        // OR 会签 + candidates 列表
            List<Long> candidates = route.getCandidates();
        if (candidates == null || candidates.isEmpty()) {
            return Result.fail(40402, "角色下找不到任何 ACTIVE 候选人");
        }
        // SkipOnLeaveRule（P1 修补 ②）
            SkipOnLeaveRule.SkipResult skipResult =
                skipOnLeaveRule.evaluate(null, candidates);
        if (skipResult.isNodeSkipped()) {
            // 全员 SKIPPED：尝试推进到下一节点（V1.3.7 标记 node_skipped=true）
            log.warn("[ApprovalService] 全员 SKIPPED，自动推进到下一节点：bizId={} skippedCount={}",
                    req.getBizId(), skipResult.getSkippedUsers().size());
        }
        // 写 sys_approval_record
            ApprovalRecord rec = new ApprovalRecord();
        rec.setBizType(req.getBizType());
        rec.setBizId(req.getBizId());
        rec.setWorkflowCode(workflowCode);
        rec.setCurrentNodeIndex(route.getMatchedNodeIndex());
        rec.setCurrentApproverUserId(skipResult.getActiveCandidates().isEmpty()
                ? route.getCurrentApproverUserId() : skipResult.getActiveCandidates().get(0));
        rec.setCandidates(serializeCandidates(skipResult.getActiveCandidates().isEmpty()
                ? candidates : skipResult.getActiveCandidates()));
        rec.setOrSignRequired(route.getOrSignRequired());
        rec.setStatus(skipResult.isNodeSkipped() ? "SKIPPED" : "PENDING");
        rec.setNodeSkipped(skipResult.isNodeSkipped());
        rec.setComment(req.getComment());
        rec.setTimeoutAt(LocalDateTime.now().plusHours(config.getTimeoutHours()));
        rec.setIsOverdue(false);
        rec.setCreateBy(req.getApplicantUserId());
        approvalMapper.insert(rec);
        // 4 通道推送
            eventPublisher.publishCreated(rec.getId(),
                skipResult.getActiveCandidates().isEmpty() ? candidates : skipResult.getActiveCandidates(),
                req.getBizType(), req.getBizId(), req.getAmount());
        return Result.ok("审批单创建成功", toVO(rec));
    }

    @Override
    public Result<PageResponse<ApprovalVO>> getPendingApprovals(Long approverUserId, int pageNum, int pageSize) {
        if (approverUserId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "approverUserId 必填");
        }
        Page<ApprovalRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ApprovalRecord> q = new LambdaQueryWrapper<>();
        q.eq(ApprovalRecord::getCurrentApproverUserId, approverUserId)
                .eq(ApprovalRecord::getStatus, "PENDING")
                .orderByDesc(ApprovalRecord::getCreateTime);
        Page<ApprovalRecord> result = approvalMapper.selectPage(page, q);
        return Result.ok(PageResponse.of(toVOs(result.getRecords()), result.getTotal(), pageNum, pageSize));
    }

    @Override
    public Result<PageResponse<ApprovalVO>> getMyPendingApprovals(Long applicantUserId, int pageNum, int pageSize) {
        if (applicantUserId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "applicantUserId 必填");
        }
        Page<ApprovalRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ApprovalRecord> q = new LambdaQueryWrapper<>();
        q.eq(ApprovalRecord::getCreateBy, applicantUserId)
                .eq(ApprovalRecord::getStatus, "PENDING")
                .orderByDesc(ApprovalRecord::getCreateTime);
        Page<ApprovalRecord> result = approvalMapper.selectPage(page, q);
        return Result.ok(PageResponse.of(toVOs(result.getRecords()), result.getTotal(), pageNum, pageSize));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "approval", action = "approval.approve")
    public Result<ApprovalVO> approve(Long approvalId, ApproveRequest req) {
        if (approvalId == null || req == null || req.getApproverUserId() == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "approvalId/approverUserId 必填");
        }
        ApprovalRecord rec = approvalMapper.selectById(approvalId);
        if (rec == null) {
            return Result.fail(40401, "审批单 " + approvalId + " 不存在");
        }
        // 状态机守卫
            if (!"PENDING".equals(rec.getStatus())) {
            throw new BizException(40904, "审批单已结束，禁止重复操作");
        }
        // 候选人守卫
            List<Long> candidates = deserializeCandidates(rec.getCandidates());
        if (!candidates.contains(req.getApproverUserId())) {
            throw new BizException(40303, "您不是本审批单的合法审批人");
        }
        // 标记通过
            rec.setStatus("APPROVED");
        rec.setComment(req.getComment());
        rec.setApprovedAt(LocalDateTime.now());
        rec.setUpdateBy(req.getApproverUserId());
        approvalMapper.updateById(rec);
        // 计算下一节点（简化：先按 OR 推进到下一节点，复杂逻辑留给 T2.2 拓展）
            Integer nextNode = rec.getCurrentNodeIndex() + 1;
        eventPublisher.publishApproved(rec.getId(), req.getApproverUserId(), nextNode,
                rec.getBizType(), rec.getBizId());
        return Result.ok("审批通过", toVO(rec));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "approval", action = "approval.reject")
    public Result<ApprovalVO> reject(Long approvalId, RejectRequest req) {
        if (approvalId == null || req == null || req.getApproverUserId() == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "approvalId/approverUserId 必填");
        }
        if (req.getReason() == null || req.getReason().trim().isEmpty()) {
            return Result.fail(40009, "驳回时 reason 必填");
        }
        ApprovalRecord rec = approvalMapper.selectById(approvalId);
        if (rec == null) {
            return Result.fail(40401, "审批单 " + approvalId + " 不存在");
        }
        if (!"PENDING".equals(rec.getStatus())) {
            throw new BizException(40904, "审批单已结束，禁止重复操作");
        }
        rec.setStatus("REJECTED");
        rec.setReason(req.getReason());
        rec.setApprovedAt(LocalDateTime.now());
        rec.setUpdateBy(req.getApproverUserId());
        approvalMapper.updateById(rec);
        eventPublisher.publishRejected(rec.getId(), req.getApproverUserId(), req.getReason(),
                rec.getBizType(), rec.getBizId());
        return Result.ok("已驳回", toVO(rec));
    }

    @Override
    public Result<Void> urge(Long approvalId, Long operatorUserId) {
        // 委托给 Watcher 处理（V1.3.7 红线：不重置 timeout_at）
            return new com.btsheng.erp.platform.auth.workflow.timeout.ApprovalTimeoutWatcher(
                approvalMapper, eventPublisher, config, null)
                .urge(approvalId, operatorUserId);
    }

    @Override
    public Result<ApprovalRecord> getApproval(Long id) {
        ApprovalRecord rec = approvalMapper.selectById(id);
        if (rec == null) {
            return Result.fail(40401, "审批单 " + id + " 不存在");
        }
        return Result.ok(rec);
    }

    /* ---------- 内部工具 ---------- */

    private String serializeCandidates(List<Long> candidates) {
        try {
            return mapper.writeValueAsString(candidates);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Long> deserializeCandidates(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ApprovalVO toVO(ApprovalRecord rec) {
        ApprovalVO vo = new ApprovalVO();
        vo.setId(rec.getId());
        vo.setBizType(rec.getBizType());
        vo.setBizId(rec.getBizId());
        vo.setWorkflowCode(rec.getWorkflowCode());
        vo.setCurrentNodeIndex(rec.getCurrentNodeIndex());
        vo.setCurrentApproverUserId(rec.getCurrentApproverUserId());
        vo.setCandidates(deserializeCandidates(rec.getCandidates()));
        vo.setOrSignRequired(rec.getOrSignRequired());
        vo.setStatus(rec.getStatus());
        vo.setSkipReason(rec.getSkipReason());
        vo.setSkippedAt(rec.getSkippedAt());
        vo.setComment(rec.getComment());
        vo.setReason(rec.getReason());
        vo.setCreatedAt(rec.getCreateTime());
        vo.setApprovedAt(rec.getApprovedAt());
        vo.setTimeoutAt(rec.getTimeoutAt());
        vo.setIsOverdue(rec.getIsOverdue());
        vo.setOverdueAt(rec.getOverdueAt());
        vo.setNodeSkipped(rec.getNodeSkipped());
        return vo;
    }

    private List<ApprovalVO> toVOs(List<ApprovalRecord> recs) {
        List<ApprovalVO> vos = new ArrayList<>();
        for (ApprovalRecord r : recs) vos.add(toVO(r));
        return vos;
    }
}
