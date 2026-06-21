package com.btsheng.erp.platform.auth.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.core.web.CurrentUserHelper;
import com.btsheng.erp.platform.auth.workflow.dto.*;
import com.btsheng.erp.platform.auth.workflow.entity.Workflow;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import com.btsheng.erp.platform.auth.workflow.enums.NodeType;
import com.btsheng.erp.platform.auth.workflow.enums.WorkflowType;
import com.btsheng.erp.platform.auth.workflow.mapper.WorkflowMapper;
import com.btsheng.erp.platform.auth.workflow.mapper.WorkflowNodeMapper;
import com.btsheng.erp.platform.auth.workflow.router.WorkflowApprovalRouter;
import com.btsheng.erp.platform.auth.workflow.validator.WorkflowValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作流 Service 实装（V1.3.7 · Story 1.2 · T2.1）
 *
 * <p>V1.3.7 字段级加密：{@code nodesJson} AES-256-GCM（{@code AesGcmTypeHandler}）。
 * 审计 100%：{@code @AuditLog} AFTER_COMMIT 钩子。
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowApprovalRouter router;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public WorkflowServiceImpl(WorkflowMapper workflowMapper,
                               WorkflowNodeMapper nodeMapper,
                               WorkflowApprovalRouter router) {
        this.workflowMapper = workflowMapper;
        this.nodeMapper = nodeMapper;
        this.router = router;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "workflow", action = "workflow.create")
    public Result<WorkflowVO> createWorkflow(WorkflowCreateRequest req) {
        if (req == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "请求体必填");
        }
        WorkflowValidator.validateCode(req.getWorkflowCode());
        if (req.getNodesJson() == null || req.getNodesJson().isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "nodesJson 必填");
        }
        // BR-1 workflow_code UNIQUE
            if (workflowMapper.findByCode(req.getWorkflowCode()) != null) {
            return Result.fail(40905, "工作流编码 " + req.getWorkflowCode() + " 已存在");
        }
        // 解析 + 校验
            List<WorkflowNode> nodes = parseNodesJson(req.getNodesJson());
        String amountField = extractAmountField(req.getConditionsJson());
        WorkflowValidator.validate(nodes, amountField);
        // 写 sys_workflow（AES-256-GCM 透明加密）+ sys_workflow_node
            Workflow wf = new Workflow();
        wf.setWorkflowCode(req.getWorkflowCode());
        wf.setNodesJson(req.getNodesJson());
        wf.setConditionsJson(req.getConditionsJson());
        wf.setStatus("ACTIVE");
        wf.setWorkflowVersion(1);
        stampAudit(wf, false);
        workflowMapper.insert(wf);
        for (WorkflowNode n : nodes) {
            n.setWorkflowId(wf.getId());
            n.setId(null);
            nodeMapper.insert(n);
        }
        log.info("[WorkflowService] 创建工作流 code={} nodes={}", req.getWorkflowCode(), nodes.size());
        return Result.ok("工作流创建成功", toVO(wf, nodes));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "workflow", action = "workflow.update")
    public Result<WorkflowVO> updateWorkflow(Long id, WorkflowUpdateRequest req) {
        if (id == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "工作流 ID 必填");
        }
        Workflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            return Result.fail(40401, "工作流 " + id + " 不存在");
        }
        if (WorkflowType.fromCode(wf.getWorkflowCode()) != null
                && WorkflowType.fromCode(wf.getWorkflowCode()).isBuiltin()) {
            // 内置模板允许修改（与 delete 不同 · BR-3 仅限制物理删除）
        }
        // 校验节点链
            List<WorkflowNode> nodes = parseNodesJson(req.getNodesJson());
        String amountField = extractAmountField(req.getConditionsJson());
        WorkflowValidator.validate(nodes, amountField);
        // 物理替换节点链
            nodeMapper.deleteByWorkflowId(id);
        for (WorkflowNode n : nodes) {
            n.setWorkflowId(id);
            n.setId(null);
            nodeMapper.insert(n);
        }
        wf.setNodesJson(req.getNodesJson());
        wf.setConditionsJson(req.getConditionsJson());
        if (req.getStatus() != null) {
            wf.setStatus(req.getStatus());
        }
        stampAudit(wf, true);
        workflowMapper.updateById(wf);
        return Result.ok("更新成功", toVO(wf, nodes));
    }

    @Override
    public Result<PageResponse<WorkflowVO>> listWorkflows(int pageNum, int pageSize, String status, String keyword) {
        Page<Workflow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Workflow> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            q.eq(Workflow::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            q.like(Workflow::getWorkflowCode, keyword);
        }
        q.orderByDesc(Workflow::getCreateTime);
        Page<Workflow> result = workflowMapper.selectPage(page, q);
        List<WorkflowVO> vos = new ArrayList<>();
        for (Workflow wf : result.getRecords()) {
            List<WorkflowNode> ns = nodeMapper.findByWorkflowIdOrderByNodeIndex(wf.getId());
            vos.add(toVO(wf, ns));
        }
        return Result.ok(PageResponse.of(vos, result.getTotal(), pageNum, pageSize));
    }

    @Override
    public Result<WorkflowVO> getWorkflow(Long id) {
        Workflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            return Result.fail(40401, "工作流 " + id + " 不存在");
        }
        List<WorkflowNode> nodes = nodeMapper.findByWorkflowIdOrderByNodeIndex(id);
        return Result.ok(toVO(wf, nodes));
    }

    @Override
    public Result<WorkflowTestResult> testWorkflow(Long id, WorkflowTestRequest req) {
        if (req == null || req.getAmount() == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "amount 必填");
        }
        Workflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            return Result.fail(40401, "工作流 " + id + " 不存在");
        }
        // 调用 router（不落库）
            WorkflowApprovalRouter.RouteResult route =
                router.route(req.getAmount(), null, wf.getWorkflowCode());
        WorkflowTestResult res = new WorkflowTestResult();
        res.setMatchedNodeIndex(route.getMatchedNodeIndex());
        res.setMatchedRole(route.getMatchedRoleCode());
        res.setCandidates(route.getCandidates());
        res.setOrSignRequired(route.getOrSignRequired());
        res.setTrace(route.getTrace());
        return Result.ok(res);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "workflow", action = "workflow.delete")
    public Result<Void> deleteWorkflow(Long id) {
        Workflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            return Result.fail(40401, "工作流 " + id + " 不存在");
        }
        WorkflowType type = WorkflowType.fromCode(wf.getWorkflowCode());
        if (type != null && type.isBuiltin()) {
            // BR-3 内置模板不可物理删除
            throw new BizException(40904, "内置工作流不可删除，请使用复制改");
        }
        // 软删（保留审计线索）
            wf.setStatus("DELETED");
        stampAudit(wf, true);
        workflowMapper.updateById(wf);
        return Result.ok("删除成功", null);
    }

    /* ---------- 内部工具 ---------- */

    private List<WorkflowNode> parseNodesJson(String json) {
        try {
            List<WorkflowNode> nodes = mapper.readValue(json, new TypeReference<List<WorkflowNode>>() {});
            // 字段归一化（seed 用 role/node，物理表用 role_code/node_index）
            for (int i = 0; i < nodes.size(); i++) {
                WorkflowNode n = nodes.get(i);
                if (n.getNodeIndex() == null) {
                    n.setNodeIndex(i + 1);
                }
            }
            return nodes;
        } catch (Exception e) {
            throw new BizException(Result.CODE_PARAM_MISSING, "nodesJson 解析失败：" + e.getMessage());
        }
    }

    private String extractAmountField(String conditionsJson) {
        if (conditionsJson == null) return null;
        try {
            return mapper.readTree(conditionsJson).path("amount_field").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void stampAudit(Workflow wf, boolean bumpVersion) {
        Long userId = CurrentUserHelper.currentUserId();
        if (userId != null) {
            wf.setLastModifiedBy(userId);
        }
        if (bumpVersion) {
            Integer ver = wf.getWorkflowVersion();
            wf.setWorkflowVersion(ver == null ? 1 : ver + 1);
        }
        LocalDateTime now = LocalDateTime.now();
        if (wf.getCreateTime() == null) {
            wf.setCreateTime(now);
        }
        wf.setUpdateTime(now);
    }

    private WorkflowVO toVO(Workflow wf, List<WorkflowNode> nodes) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(wf.getId());
        vo.setName(wf.getName());
        vo.setWorkflowCode(wf.getWorkflowCode());
        vo.setConditionsJson(wf.getConditionsJson());
        vo.setStatus(wf.getStatus());
        vo.setVersion(wf.getWorkflowVersion());
        vo.setLastModifiedBy(wf.getLastModifiedBy());
        vo.setCreateTime(wf.getCreateTime());
        vo.setUpdateTime(wf.getUpdateTime());
        List<WorkflowVO.WorkflowNodeVO> nodeVOs = new ArrayList<>();
        for (WorkflowNode n : nodes) {
            WorkflowVO.WorkflowNodeVO nv = new WorkflowVO.WorkflowNodeVO();
            nv.setNodeIndex(n.getNodeIndex());
            nv.setNodeType(n.getNodeType());
            nv.setRoleCode(n.getRoleCode());
            nv.setThreshold(n.getThreshold());
            nv.setOrSignRequired(n.getOrSignRequired());
            nodeVOs.add(nv);
        }
        vo.setNodes(nodeVOs);
        return vo;
    }
}
