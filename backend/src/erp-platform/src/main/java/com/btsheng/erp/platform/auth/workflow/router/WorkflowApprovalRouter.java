package com.btsheng.erp.platform.auth.workflow.router;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import com.btsheng.erp.platform.auth.workflow.enums.NodeType;
import com.btsheng.erp.platform.auth.workflow.mapper.WorkflowNodeMapper;
import com.btsheng.erp.platform.auth.workflow.mapper.WorkflowMapper;
import com.btsheng.erp.platform.auth.workflow.entity.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 升级版工作流路由（V1.3.7 · Story 1.2 · T1.2 · P1 修补 ①）
 *
 * <p>升级点（对比 Story 1.1 QuoteApprovalRouter）：
 * <ul>
 *   <li>新增 {@code workflowCode} 参数，实时读 {@code sys_workflow_node} 链（不再硬编码 salesperson/dept_manager/gm）</li>
 *   <li>返回 {@code candidates: List<Long>}（不再 LIMIT 1）</li>
 *   <li>返回 {@code orSignRequired: Boolean}（V1.3.7 P1 修补 OR 会签透出）</li>
 *   <li>返回 {@code matchedNodeIndex: int} / {@code trace: List<String>}</li>
 * </ul>
 *
 * <p>2 参重载保留（兼容 Story 1.1 35 测例不破 · architect P2 反馈 ②）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Service
public class WorkflowApprovalRouter {

    private static final Logger log = LoggerFactory.getLogger(WorkflowApprovalRouter.class);

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final Story11CompatRouter story11Compat;

    @Autowired
    public WorkflowApprovalRouter(WorkflowMapper workflowMapper,
                                  WorkflowNodeMapper nodeMapper,
                                  Story11CompatRouter story11Compat) {
        this.workflowMapper = workflowMapper;
        this.nodeMapper = nodeMapper;
        this.story11Compat = story11Compat;
    }

    /**
     * 2 参重载（兼容 Story 1.1 35 测例 · architect P2 反馈 ②）。
     */
    public com.btsheng.erp.platform.auth.dto.QuoteApprovalResult route(BigDecimal amount, Long applicantUserId) {
        return story11Compat.route(amount, applicantUserId);
    }

    /**
     * 3 参路由决策（V1.3.7 Story 1.2 主入口）。
     *
     * @param amount    金额
     * @param applicantUserId 申请人 user_id
     * @param workflowCode 工作流编码（sys_workflow.workflow_code）
     * @return {@link RouteResult}
     */
    public RouteResult route(BigDecimal amount, Long applicantUserId, String workflowCode) {
        if (amount == null) {
            throw new BizException(Result.CODE_PARAM_MISSING, "金额必填");
        }
        if (workflowCode == null) {
            throw new BizException(Result.CODE_PARAM_MISSING, "workflowCode 必填");
        }
        Workflow wf = workflowMapper.findActiveByCode(workflowCode);
        if (wf == null) {
            throw new BizException(40401, "工作流 " + workflowCode + " 不存在或非 ACTIVE");
        }
        List<WorkflowNode> nodes = nodeMapper.findByWorkflowIdOrderByNodeIndex(wf.getId());
        if (nodes.isEmpty()) {
            throw new BizException(Result.CODE_NOT_FOUND_ROUTING,
                    "工作流 " + workflowCode + " 无节点配置");
        }
        List<String> trace = new ArrayList<>();
        for (WorkflowNode n : nodes) {
            NodeType type = NodeType.fromCode(n.getNodeType());
            if (type != NodeType.APPROVAL) {
                trace.add("node " + n.getNodeIndex() + " " + n.getNodeType() + " (skip)");
                continue;
            }
            BigDecimal th = n.getThreshold();
            // 严格大于：amount > threshold 才进入下一节点
            boolean inScope = (th == null) || (amount.compareTo(th) <= 0);
            trace.add("node " + n.getNodeIndex() + " " + n.getRoleCode()
                    + " threshold=" + (th == null ? "NULL" : th.toPlainString())
                    + " inScope=" + inScope);
            if (inScope) {
                // 命中：取该角色下有效候选人（这里委托 story11Compat 的角色查询保持兼容）
            List<Long> candidates = story11Compat.findActiveUserIdsByRoleCode(n.getRoleCode());
                if (candidates.isEmpty()) {
                    // 角色下找不到任何 ACTIVE 候选人
            if ("GM".equalsIgnoreCase(n.getRoleCode())) {
                        throw new BizException(Result.CODE_NOT_FOUND_ROUTING,
                                "未找到 GM 角色有效用户，请联系管理员");
                    }
                    // 非 GM 角色找不到候选人 → 继续下一节点（向上抛）
            continue;
                }
                Collections.sort(candidates);
                RouteResult res = new RouteResult();
                res.setMatchedNodeIndex(n.getNodeIndex());
                res.setMatchedRoleCode(n.getRoleCode());
                res.setCandidates(candidates);
                res.setOrSignRequired(Boolean.TRUE.equals(n.getOrSignRequired()));
                // BR-10 首次分配确定性：取 candidates[0]
            res.setCurrentApproverUserId(candidates.get(0));
                res.setTrace(trace);
                if (log.isDebugEnabled()) {
                    log.debug("[WorkflowApprovalRouter] 路由命中：amount={} → node={} role={} candidates={}",
                            amount, n.getNodeIndex(), n.getRoleCode(), candidates);
                }
                return res;
            }
        }
        throw new BizException(Result.CODE_NOT_FOUND_ROUTING, "未找到合适的审批人，请联系管理员");
    }

    /**
     * 路由结果（V1.3.7 P1 修补 · OR 会签字段透出）。
     */
    public static class RouteResult {
        private int matchedNodeIndex;
        private String matchedRoleCode;
        private List<Long> candidates;
        private Long currentApproverUserId;
        private Boolean orSignRequired = false;
        private List<String> trace;

        public int getMatchedNodeIndex() { return matchedNodeIndex; }
        public void setMatchedNodeIndex(int matchedNodeIndex) { this.matchedNodeIndex = matchedNodeIndex; }
        public String getMatchedRoleCode() { return matchedRoleCode; }
        public void setMatchedRoleCode(String matchedRoleCode) { this.matchedRoleCode = matchedRoleCode; }
        public List<Long> getCandidates() { return candidates; }
        public void setCandidates(List<Long> candidates) { this.candidates = candidates; }
        public Long getCurrentApproverUserId() { return currentApproverUserId; }
        public void setCurrentApproverUserId(Long currentApproverUserId) { this.currentApproverUserId = currentApproverUserId; }
        public Boolean getOrSignRequired() { return orSignRequired; }
        public void setOrSignRequired(Boolean orSignRequired) { this.orSignRequired = orSignRequired; }
        public List<String> getTrace() { return trace; }
        public void setTrace(List<String> trace) { this.trace = trace; }
    }
}
