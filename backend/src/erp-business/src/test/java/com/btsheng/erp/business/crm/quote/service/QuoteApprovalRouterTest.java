package com.btsheng.erp.business.crm.quote.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** V1.3.7 Story 1.5 · AC-2.2.2 · T2.1/T2.2 · QuoteApprovalRouter 测例 (4 测例) */
class QuoteApprovalRouterTest {

    private final QuoteApprovalRouter router = new QuoteApprovalRouter();

    @Test void route_3k_salesperson() {
        assertEquals("SELF", router.routeDecision(new BigDecimal("3000")));
    }

    @Test void route_50k_dept_manager_or_sign() {
        assertEquals("DEPT_MANAGER_OR_SIGN", router.routeDecision(new BigDecimal("50000")));
    }

    @Test void route_250k_gm_finance_dual_sign() {
        assertEquals("GM_FINANCE_DUAL_SIGN", router.routeDecision(new BigDecimal("250000")));
    }

    @Test void route_300k_boundary() {
        assertEquals("GM_FINANCE_DUAL_SIGN", router.routeDecision(new BigDecimal("300000")));
        assertTrue(router.isBoundaryCase(new BigDecimal("300000")));
    }
}
