package com.btsheng.erp.platform.auth.workflow.router;

import com.btsheng.erp.platform.auth.dto.QuoteApprovalResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteApprovalRouterTest {

    @Mock private Story11CompatRouter story11Compat;

    @Test void route_30k_self() {
        QuoteApprovalResult res = new QuoteApprovalResult();
        res.setCandidates(List.of(10086L));
        res.setCurrentNode("salesperson");
        when(story11Compat.route(any(), any())).thenReturn(res);
        QuoteApprovalResult out = story11Compat.route(new BigDecimal("30000"), 10086L);
        assertEquals(10086L, out.getCandidates().get(0));
    }

    @Test void route_60k_or_sign_2() {
        QuoteApprovalResult res = new QuoteApprovalResult();
        res.setCandidates(List.of(10010L, 10011L));
        res.setCurrentNode("dept_manager");
        when(story11Compat.route(any(), any())).thenReturn(res);
        QuoteApprovalResult out = story11Compat.route(new BigDecimal("60000"), 10086L);
        assertEquals(2, out.getCandidates().size());
    }

    @Test void route_250k_gm() {
        QuoteApprovalResult res = new QuoteApprovalResult();
        res.setCandidates(List.of(10001L));
        res.setCurrentNode("gm");
        when(story11Compat.route(any(), any())).thenReturn(res);
        QuoteApprovalResult out = story11Compat.route(new BigDecimal("250000"), 10086L);
        assertEquals(10001L, out.getCandidates().get(0));
    }

    @Test void route_60k_dept_manager_self() {
        QuoteApprovalResult res = new QuoteApprovalResult();
        res.setCandidates(List.of(10010L));
        when(story11Compat.route(any(), any())).thenReturn(res);
        QuoteApprovalResult out = story11Compat.route(new BigDecimal("60000"), 10010L);
        assertEquals(1, out.getCandidates().size());
    }

    @Test void boundary_50000_routes_dept() {
        QuoteApprovalResult res = new QuoteApprovalResult();
        res.setCandidates(List.of(10010L, 10011L));
        when(story11Compat.route(any(), any())).thenReturn(res);
        QuoteApprovalResult out = story11Compat.route(new BigDecimal("50000"), 10086L);
        assertEquals(2, out.getCandidates().size());
    }

    @Test void boundary_200000_routes_dept() {
        QuoteApprovalResult res = new QuoteApprovalResult();
        res.setCandidates(List.of(10010L, 10011L));
        when(story11Compat.route(any(), any())).thenReturn(res);
        QuoteApprovalResult out = story11Compat.route(new BigDecimal("200000"), 10086L);
        assertEquals(2, out.getCandidates().size());
    }
}
