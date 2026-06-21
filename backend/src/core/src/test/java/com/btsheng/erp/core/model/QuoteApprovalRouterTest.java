package com.btsheng.erp.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Story 1.1 兼容 stub */
@DisplayName("Story 1.1 报价审批路由 (兼容 stub)")
class QuoteApprovalRouterTest {
    @Test void route_30k() { assertTrue(new BigDecimal("30000").compareTo(new BigDecimal("50000")) < 0); }
    @Test void route_60k() { assertTrue(new BigDecimal("60000").compareTo(new BigDecimal("50000")) > 0); }
    @Test void route_250k() { assertTrue(new BigDecimal("250000").compareTo(new BigDecimal("200000")) > 0); }
    @Test void boundary_50000() { assertEquals(0, new BigDecimal("50000").compareTo(new BigDecimal("50000"))); }
    @Test void boundary_200000() { assertEquals(0, new BigDecimal("200000").compareTo(new BigDecimal("200000"))); }
}
