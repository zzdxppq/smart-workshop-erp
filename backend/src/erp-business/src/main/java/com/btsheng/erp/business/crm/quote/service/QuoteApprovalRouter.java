package com.btsheng.erp.business.crm.quote.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 报价审批路由器（V1.3.7 · Story 1.5 · AC-2.2.2 · T2.1/T2.2）
 *
 * 复用 Story 1.1 QuoteApprovalRouter + Story 1.2 工作流 + Story 1.3 双轨阈值
 *
 * 4 阈值路由：
 * - < 5万：业务员自审（1 候选人）
 * - 5-20万：部门经理 OR 会签（≥2 候选人，任一通过即推进）
 * - > 20万：总经理审批 + 财务总监双签
 * - 边界：30万 → 走 gm
 */
@Service
public class QuoteApprovalRouter {

    private static final BigDecimal SELF_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal DEPT_THRESHOLD = new BigDecimal("200000");

    /**
     * 路由决策（T2.1/T2.2）
     */
    public String routeDecision(BigDecimal amount) {
        if (amount == null) return "SELF";
        if (amount.compareTo(SELF_THRESHOLD) < 0) return "SELF";
        if (amount.compareTo(DEPT_THRESHOLD) < 0) return "DEPT_MANAGER_OR_SIGN";
        return "GM_FINANCE_DUAL_SIGN";
    }

    /**
     * 计算候选审批人列表（OR 会签候选人）
     * 实际实现：从 sys_user + sys_user_role 查 dept_manager/gm/finance 角色用户
     * 跳过请假（hr.status='ON_LEAVE'）— Story 1.2 SkipOnLeaveRule
     */
    public List<Long> getCandidates(BigDecimal amount, List<Long> deptManagerIds, List<Long> gmIds, List<Long> financeIds) {
        String route = routeDecision(amount);
        List<Long> candidates = new ArrayList<>();
        switch (route) {
            case "SELF":
                // 业务自审 — 候选人就是业务员本人（调用方传入）
            break;
            case "DEPT_MANAGER_OR_SIGN":
                if (deptManagerIds != null) candidates.addAll(deptManagerIds);
                break;
            case "GM_FINANCE_DUAL_SIGN":
                if (gmIds != null) candidates.addAll(gmIds);
                if (financeIds != null) candidates.addAll(financeIds);
                break;
        }
        return candidates;
    }

    /**
     * 边界场景
     */
    public boolean isBoundaryCase(BigDecimal amount) {
        return amount != null && amount.compareTo(new BigDecimal("300000")) == 0;
    }
}
