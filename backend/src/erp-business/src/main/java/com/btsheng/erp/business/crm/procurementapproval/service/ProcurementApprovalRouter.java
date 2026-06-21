package com.btsheng.erp.business.crm.procurementapproval.service;

import com.btsheng.erp.business.crm.noorderpurchase.service.NoOrderPurchaseService;
import com.btsheng.erp.business.crm.procurementapproval.dto.ApprovalRouteRequest;
import com.btsheng.erp.business.crm.procurementapproval.dto.ApprovalRouteResponse;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * V1.3.8 · Story 4.2 · 采购主管审批路由 Service
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #previewRoute} AC-4.2.2 路由预览（无副作用）</li>
 * </ul>
 *
 * <p>路由决策（与 V52 sys_workflow_node 扩展一致）：
 * <ul>
 *   <li>金额 ≤ 1 万：SELF（业务自审）</li>
 *   <li>金额 1-5 万：PROCUREMENT_MANAGER</li>
 *   <li>金额 > 5 万：GM + PROCUREMENT_MANAGER 双签</li>
 *   <li>品类 TOOL/CHEMICAL：PROCUREMENT_MANAGER</li>
 *   <li>紧急度 URGENT + 金额 > 1 万：PROCUREMENT_MANAGER 必审</li>
 * </ul>
 *
 * <p>precheck 校正（2026-06-13）：V1.3.7 sys_workflow_node 仅支持金额阈值，
 * 品类/紧急度分支走应用层 if 判断。V52 迁移已校正（详见 docs/dev/logs/4.2-precheck.log）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Service
public class ProcurementApprovalRouter {

    private static final Logger log = LoggerFactory.getLogger(ProcurementApprovalRouter.class);

    private final WorkflowEventService workflowEventService;

    @Autowired
    public ProcurementApprovalRouter(WorkflowEventService workflowEventService) {
        this.workflowEventService = workflowEventService;
    }

    /** 阈值常量（与 NoOrderPurchaseService 保持一致，可由 dev 后续抽出常量类） */
    public static final BigDecimal AMOUNT_PM_THRESHOLD = NoOrderPurchaseService.AMOUNT_PM_THRESHOLD;
    public static final BigDecimal AMOUNT_GM_THRESHOLD = NoOrderPurchaseService.AMOUNT_GM_THRESHOLD;

    /** 阈值标识常量（用于 matchedThresholds 字段） */
    public static final String T_AMOUNT_BELOW_10K = "AMOUNT_BELOW_10K";
    public static final String T_AMOUNT_10K_50K = "AMOUNT_10K_50K";
    public static final String T_AMOUNT_ABOVE_50K = "AMOUNT_ABOVE_50K";
    public static final String T_CATEGORY_TOOL = "CATEGORY_TOOL";
    public static final String T_CATEGORY_CHEMICAL = "CATEGORY_CHEMICAL";
    public static final String T_URGENCY_HIGH_AMOUNT = "URGENCY_URGENT_AMOUNT_OVER_10K";

    /** 路由角色常量 */
    public static final String ROLE_PM = "PROCUREMENT_MANAGER";
    public static final String ROLE_GM = "GM";
    public static final String ROLE_DEPT = "DEPT_MANAGER"; // V1.3.7 legacy

    /**
     * AC-4.2.2：路由预览（无副作用）
     */
    public Result<ApprovalRouteResponse> previewRoute(ApprovalRouteRequest req) {
        if (req == null || req.getAmount() == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "amount 必填");
        }

        Set<String> matched = new LinkedHashSet<>();
        Set<String> routeSet = new LinkedHashSet<>();
        List<String> legacyRoute = new ArrayList<>();

        BigDecimal amount = req.getAmount();

        // ========== 主路由：金额阈值 ==========
            if (amount.compareTo(AMOUNT_PM_THRESHOLD) < 0) {
            // ≤ 1 万：业务自审，不进审批链
            matched.add(T_AMOUNT_BELOW_10K);
        } else if (amount.compareTo(AMOUNT_GM_THRESHOLD) < 0) {
            // 1-5 万：PROCUREMENT_MANAGER
            matched.add(T_AMOUNT_10K_50K);
            routeSet.add(ROLE_PM);
            legacyRoute.add(ROLE_DEPT); // V1.3.7 兼容
        } else {
            // > 5 万：GM + PROCUREMENT_MANAGER 双签
            matched.add(T_AMOUNT_ABOVE_50K);
            routeSet.add(ROLE_PM);
            routeSet.add(ROLE_GM);
            legacyRoute.add(ROLE_GM);
        }

        // ========== 附加路由：品类 ==========
        // precheck 校正：sys_workflow_node 不支持品类阈值，应用层 if
            if ("TOOL".equalsIgnoreCase(req.getCategory())) {
            matched.add(T_CATEGORY_TOOL);
            routeSet.add(ROLE_PM);
        } else if ("CHEMICAL".equalsIgnoreCase(req.getCategory())) {
            matched.add(T_CATEGORY_CHEMICAL);
            routeSet.add(ROLE_PM);
        }

        // ========== 附加路由：紧急度 ==========
            if ("URGENT".equalsIgnoreCase(req.getUrgency())
                && amount.compareTo(AMOUNT_PM_THRESHOLD) > 0) {
            matched.add(T_URGENCY_HIGH_AMOUNT);
            routeSet.add(ROLE_PM);
        }

        ApprovalRouteResponse resp = new ApprovalRouteResponse();
        resp.setRoute(new ArrayList<>(routeSet));
        resp.setMatchedThresholds(new ArrayList<>(matched));
        resp.setEstimatedSigners(routeSet.size());
        resp.setCompatibleLegacyRoute(legacyRoute.isEmpty() ? null : legacyRoute);

        log.info("[ProcurementApprovalRouter] previewRoute: amount={} category={} urgency={} → route={} matched={}",
                amount, req.getCategory(), req.getUrgency(), routeSet, matched);

        // V1.3.8 Sprint 9 Story 9.1：记录 workflow_event PREVIEWED（无副作用异步）
            recordPreviewEvent(amount, req, routeSet, matched);

        return Result.ok(resp);
    }

    /**
     * V1.3.8 Sprint 9 Story 9.1：异步记录预览事件（不影响路由决策主流程）
     *
     * <p>异常吞掉：审计日志不影响业务
     */
    private void recordPreviewEvent(BigDecimal amount, ApprovalRouteRequest req,
                                   Set<String> routeSet, Set<String> matched) {
        // V1.3.8 Sprint 9 Story 9.1 简化：避免 intValue() NPE，matchThreshold 拼接而非拆箱
            try {
            String primaryRole = routeSet.isEmpty() ? "SELF" : routeSet.iterator().next();
            String matchedThreshold = matched.isEmpty() ? null : matched.iterator().next();
            int matchedNodeIndex = matched.contains("AMOUNT_ABOVE_50K") ? 6
                    : matched.contains("AMOUNT_10K_50K") ? 5 : 0;

            workflowEventService.recordEvent(
                    "PO_APPROVAL", null, null,
                    "PREVIEWED",
                    primaryRole,
                    null, null,
                    "预览路由 amount=" + amount,
                    matchedNodeIndex == 0 ? null : matchedNodeIndex,
                    matchedThreshold);
        } catch (Exception e) {
            log.warn("[ProcurementApprovalRouter] recordPreviewEvent failed: {}", e.getMessage());
        }
    }

    /**
     * AC-4.2.1：PROCUREMENT_MANAGER 角色权限列表（V52 迁移 INSERT IGNORE）
     * <p>本期：硬编码返回（V52 已落库，应用层只读返回）
     */
    public Result<List<String>> getProcurementManagerPermissions() {
        return Result.ok(Arrays.asList(
                "purchase:approval:read",
                "purchase:approval:approve",
                "purchase:approval:reject",
                "purchase:no-order:create"
        ));
    }
}