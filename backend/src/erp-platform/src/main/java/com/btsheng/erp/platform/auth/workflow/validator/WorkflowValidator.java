package com.btsheng.erp.platform.auth.workflow.validator;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import com.btsheng.erp.platform.auth.workflow.enums.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作流校验器（V1.3.7 · Story 1.2 · T1.4 · RED→GREEN）
 *
 * <p>校验规则（与 AC-1.2.1 BR-2/BR-4 + Data Validation 严格对齐）：
 * <ol>
 *   <li>节点数 ∈ [2, 20]</li>
 *   <li>首节点 type=START、末节点 type=END</li>
 *   <li>node_index 严格 1..N 递增（无重复、无跳号）</li>
 *   <li>阈值单调非降（threshold[i] ≤ threshold[i+1]，NULL 视作 +∞）</li>
 *   <li>APPROVAL 节点 roleCode 必填（不强制查 sys_role，由 Service 层校验）</li>
 * </ol>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public final class WorkflowValidator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowValidator.class);

    private WorkflowValidator() {
    }

    /**
     * 校验节点链合法性。
     *
     * @param nodes     节点列表
     * @param amountField conditions_json 中的 amount_field（必填）
     * @throws BizException 40001/40003/40004/40005/40006 各类错误
     */
    public static void validate(List<WorkflowNode> nodes, String amountField) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BizException(Result.CODE_PARAM_MISSING, "节点列表为空");
        }
        // 1. 节点数 ∈ [2, 20]
            if (nodes.size() < 2 || nodes.size() > 20) {
            throw new BizException(40005, "节点数必须在 2-20 之间");
        }
        // 2. 首节点 = START，末节点 = END
            NodeType first = NodeType.fromCode(nodes.get(0).getNodeType());
        NodeType last = NodeType.fromCode(nodes.get(nodes.size() - 1).getNodeType());
        if (first != NodeType.START) {
            throw new BizException(40003, "首节点必须为 START");
        }
        if (last != NodeType.END) {
            throw new BizException(40004, "末节点必须为 END");
        }
        // 3. node_index 严格 1..N 递增
            Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNode n = nodes.get(i);
            if (n.getNodeIndex() == null) {
                throw new BizException(Result.CODE_PARAM_MISSING, "节点 " + (i + 1) + " node_index 为空");
            }
            if (n.getNodeIndex() != i + 1) {
                throw new BizException(Result.CODE_PARAM_MISSING,
                        "node_index 必须严格 1..N 递增（实际 " + n.getNodeIndex() + "，期望 " + (i + 1) + "）");
            }
            if (!seen.add(n.getNodeIndex())) {
                throw new BizException(Result.CODE_PARAM_MISSING, "node_index 重复：" + n.getNodeIndex());
            }
        }
        // 4. 阈值单调非降（仅校验 APPROVAL 节点链；CC 节点无 threshold）
            BigDecimal prev = null;
        for (WorkflowNode n : nodes) {
            NodeType t = NodeType.fromCode(n.getNodeType());
            if (t == NodeType.APPROVAL) {
                BigDecimal cur = n.getThreshold();
                if (cur != null) {
                    if (prev != null && cur.compareTo(prev) < 0) {
                        throw new BizException(40001, "节点阈值必须单调非降：" + prev + " > " + cur);
                    }
                    prev = cur;
                }
            }
        }
        // 5. APPROVAL 节点必须有 roleCode
            for (WorkflowNode n : nodes) {
            NodeType t = NodeType.fromCode(n.getNodeType());
            if (t == NodeType.APPROVAL && (n.getRoleCode() == null || n.getRoleCode().isEmpty())) {
                throw new BizException(Result.CODE_PARAM_MISSING, "APPROVAL 节点必须设置 roleCode");
            }
        }
        // 6. conditions.amount_field 必填
            if (amountField == null || amountField.isEmpty()) {
            throw new BizException(40006, "conditions_json.amount_field 必填");
        }
        if (log.isDebugEnabled()) {
            log.debug("[WorkflowValidator] 节点链校验通过：{} 节点", nodes.size());
        }
    }

    /**
     * 验证 workflow_code 格式。
     *
     * @param code 工作流编码
     */
    public static void validateCode(String code) {
        if (code == null || !code.matches("^[A-Z_]{2,50}$")) {
            throw new BizException(Result.CODE_PARAM_MISSING, "workflow_code 必须 ^[A-Z_]{2,50}$");
        }
    }
}
