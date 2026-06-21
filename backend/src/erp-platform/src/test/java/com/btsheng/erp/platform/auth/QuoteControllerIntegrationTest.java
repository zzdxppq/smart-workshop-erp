package com.btsheng.erp.platform.auth;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.controller.QuoteController;
import com.btsheng.erp.platform.auth.dto.QuoteApprovalResult;
import com.btsheng.erp.platform.auth.service.QuoteApprovalRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QuoteController 路由消费测试（V1.3.7 · T3.3 · AC-1.1.3 集成）
 */
@DisplayName("QuoteController 路由消费")
class QuoteControllerIntegrationTest {

    private QuoteApprovalRouter router;
    private QuoteController controller;

    @BeforeEach
    void setUp() {
        router = Mockito.mock(QuoteApprovalRouter.class);
        controller = new QuoteController(router);
    }

    @Test
    @DisplayName("submit_60k_salesperson: 60000 → dept_manager 10010")
    void submit_60k_salesperson() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setApproverUserId(10010L);
        r.setCurrentNode("dept_manager");
        r.setCandidates(List.of(10010L, 10011L));
        r.setReason("金额 60000 > 50000 → dept_manager");
        Mockito.when(router.route(Mockito.any(), Mockito.anyLong())).thenReturn(r);

        QuoteController.QuoteSubmitRequest req = new QuoteController.QuoteSubmitRequest();
        req.setAmount(new BigDecimal("60000"));
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader("X-User-Id", "10086");
        Result<Map<String, Object>> resp = controller.submit(req, http);

        assertEquals(0, resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(10010L, resp.getData().get("approverUserId"));
        assertEquals("dept_manager", resp.getData().get("currentNode"));
    }

    @Test
    @DisplayName("submit_30k_salesperson: 30000 → 自审 10086")
    void submit_30k_salesperson() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setApproverUserId(10086L);
        r.setCurrentNode("salesperson");
        r.setCandidates(List.of(10086L));
        Mockito.when(router.route(Mockito.any(), Mockito.anyLong())).thenReturn(r);

        QuoteController.QuoteSubmitRequest req = new QuoteController.QuoteSubmitRequest();
        req.setAmount(new BigDecimal("30000"));
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader("X-User-Id", "10086");
        Result<Map<String, Object>> resp = controller.submit(req, http);
        assertEquals(10086L, resp.getData().get("approverUserId"));
        assertTrue(((List<?>) resp.getData().get("candidates")).contains(10086L));
    }
}
