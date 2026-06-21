package com.btsheng.erp.platform.auth.workflow.util;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.workflow.entity.Workflow;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import com.btsheng.erp.platform.auth.workflow.enums.NodeType;
import com.btsheng.erp.platform.auth.workflow.enums.WorkflowType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 工作流模板工厂（V1.3.7 · Story 1.2 · T1.1 · RED→GREEN）
 *
 * <p>提供 4 套内置模板（与 {@code init.sql:1080-1085} 严格一致）：
 * <ul>
 *   <li>QUOTE_FLOW    - 报价审批（OWNER 5万 / SALES_MGR 20万 / GM NULL）</li>
 *   <li>ORDER_FLOW    - 订单审批（同 QUOTE + extra_check=credit_limit）</li>
 *   <li>PURCHASE_FLOW - 采购审批（BUYER 1万 / SALES_MGR 5万 / GM NULL）</li>
 *   <li>PAYMENT_FLOW  - 付款审批（FINANCE 10万 / GM NULL，dual_sign=&gt;10万）</li>
 * </ul>
 *
 * <p>公开方法：
 * <ul>
 *   <li>{@link #getTemplate(WorkflowType)} - 取 4 套模板节点链</li>
 *   <li>{@link #cloneTemplate(WorkflowType, String)} - "复制改"产生新 workflow</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public final class WorkflowTemplateFactory {

    private WorkflowTemplateFactory() {
    }

    /**
     * 取 4 套模板节点链（V1.3.7 与 init.sql 严格一致）。
     *
     * @param type 工作流类型
     * @return 节点列表（含 START + APPROVAL* + END）
     * @throws BizException 40006 当 type 非 4 套内置模板之一
     */
    public static List<WorkflowNode> getTemplate(WorkflowType type) {
        if (type == null) {
            throw new BizException(Result.CODE_PARAM_MISSING, "工作流类型必填");
        }
        switch (type) {
            case QUOTE_FLOW:
                return buildQuote();
            case ORDER_FLOW:
                return buildOrder();
            case PURCHASE_FLOW:
                return buildPurchase();
            case PAYMENT_FLOW:
                return buildPayment();
            default:
                throw new BizException(Result.CODE_PARAM_MISSING, "工作流类型不支持：" + type);
        }
    }

    /**
     * "复制改"产生新 workflow（V1.3.7 留版本号字段）。
     *
     * @param source   源工作流类型
     * @param newCode  新 workflow_code（V1.3.7 唯一约束）
     * @return 新 Workflow 对象（version=v1，status=ACTIVE）
     */
    public static Workflow cloneTemplate(WorkflowType source, String newCode) {
        if (source == null) {
            throw new BizException(Result.CODE_PARAM_MISSING, "源工作流类型必填");
        }
        if (newCode == null || !newCode.matches("^[A-Z_]{2,50}$")) {
            throw new BizException(Result.CODE_PARAM_MISSING, "新编码必须 ^[A-Z_]{2,50}$");
        }
        if (source == WorkflowType.CUSTOM_FLOW) {
            throw new BizException(Result.CODE_PARAM_MISSING, "CUSTOM_FLOW 不可作为复制源");
        }
        List<WorkflowNode> nodes = getTemplate(source);
        // 序列化为 JSON（AES-256-GCM 加密在 Service 层做）
            StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(",");
            WorkflowNode n = nodes.get(i);
            sb.append("{\"node_index\":").append(n.getNodeIndex())
              .append(",\"node_type\":\"").append(n.getNodeType()).append("\"");
            if (n.getRoleCode() != null) {
                sb.append(",\"role_code\":\"").append(n.getRoleCode()).append("\"");
            }
            if (n.getThreshold() != null) {
                sb.append(",\"threshold\":").append(n.getThreshold().toPlainString());
            }
            if (n.getOrSignRequired() != null) {
                sb.append(",\"or_sign_required\":").append(n.getOrSignRequired());
            }
            sb.append("}");
        }
        sb.append("]");

        Workflow wf = new Workflow();
        wf.setWorkflowCode(newCode);
        wf.setNodesJson(sb.toString());
        wf.setConditionsJson(buildConditions(source));
        wf.setStatus("ACTIVE");
        wf.setWorkflowVersion(1);
        return wf;
    }

    /** QUOTE_FLOW：OWNER 5万 / SALES_MGR 20万 / GM NULL（3 APPROVAL + START + END） */
    private static List<WorkflowNode> buildQuote() {
        List<WorkflowNode> ns = new ArrayList<>();
        ns.add(node(1, NodeType.START, null, null, false));
        ns.add(node(2, NodeType.APPROVAL, "OWNER", new BigDecimal("50000"), false));
        ns.add(node(3, NodeType.APPROVAL, "SALES_MGR", new BigDecimal("200000"), false));
        ns.add(node(4, NodeType.APPROVAL, "GM", null, false));
        ns.add(node(5, NodeType.END, null, null, false));
        return ns;
    }

    /** ORDER_FLOW：同 QUOTE + extra_check=credit_limit */
    private static List<WorkflowNode> buildOrder() {
        List<WorkflowNode> ns = new ArrayList<>();
        WorkflowNode n1 = node(1, NodeType.START, null, null, false);
        WorkflowNode n2 = node(2, NodeType.APPROVAL, "OWNER", new BigDecimal("50000"), false);
        n2.setExtraCheckJson("{\"extra_check\":\"credit_limit\"}");
        WorkflowNode n3 = node(3, NodeType.APPROVAL, "SALES_MGR", new BigDecimal("200000"), false);
        n3.setExtraCheckJson("{\"extra_check\":\"credit_limit\"}");
        WorkflowNode n4 = node(4, NodeType.APPROVAL, "GM", null, false);
        n4.setExtraCheckJson("{\"extra_check\":\"credit_limit\"}");
        ns.add(n1);
        ns.add(n2);
        ns.add(n3);
        ns.add(n4);
        ns.add(node(5, NodeType.END, null, null, false));
        return ns;
    }

    /** PURCHASE_FLOW：BUYER 1万 / SALES_MGR 5万 / GM NULL */
    private static List<WorkflowNode> buildPurchase() {
        List<WorkflowNode> ns = new ArrayList<>();
        ns.add(node(1, NodeType.START, null, null, false));
        ns.add(node(2, NodeType.APPROVAL, "BUYER", new BigDecimal("10000"), false));
        ns.add(node(3, NodeType.APPROVAL, "SALES_MGR", new BigDecimal("50000"), false));
        ns.add(node(4, NodeType.APPROVAL, "GM", null, false));
        ns.add(node(5, NodeType.END, null, null, false));
        return ns;
    }

    /** PAYMENT_FLOW：FINANCE 10万 / GM NULL（dual_sign=>10万） */
    private static List<WorkflowNode> buildPayment() {
        List<WorkflowNode> ns = new ArrayList<>();
        ns.add(node(1, NodeType.START, null, null, false));
        ns.add(node(2, NodeType.APPROVAL, "FINANCE", new BigDecimal("100000"), false));
        ns.add(node(3, NodeType.APPROVAL, "GM", null, false));
        ns.add(node(4, NodeType.END, null, null, false));
        return ns;
    }

    private static String buildConditions(WorkflowType type) {
        switch (type) {
            case ORDER_FLOW:
                return "{\"amount_field\":\"total_amount\",\"extra_check\":\"credit_limit\"}";
            case PURCHASE_FLOW:
                return "{\"amount_field\":\"amount\"}";
            case PAYMENT_FLOW:
                return "{\"amount_field\":\"amount\",\"dual_sign\":\">100000\"}";
            case QUOTE_FLOW:
            default:
                return "{\"amount_field\":\"total_amount\"}";
        }
    }

    /** 节点构造（私有） */
    private static WorkflowNode node(int idx, NodeType type, String role, BigDecimal threshold, boolean orSign) {
        WorkflowNode n = new WorkflowNode();
        n.setNodeIndex(idx);
        n.setNodeType(type.getCode());
        n.setRoleCode(role);
        n.setThreshold(threshold);
        n.setOrSignRequired(orSign);
        return n;
    }
}
