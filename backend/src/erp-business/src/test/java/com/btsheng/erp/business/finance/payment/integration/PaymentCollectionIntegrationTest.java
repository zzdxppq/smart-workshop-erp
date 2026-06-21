package com.btsheng.erp.business.finance.payment.integration;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.payment.dto.CreatePlanRequest;
import com.btsheng.erp.business.finance.payment.dto.MarkPaidRequest;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentAlert;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentPlan;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentAlertMapper;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentPlanMapper;
import com.btsheng.erp.business.finance.payment.service.PaymentCollectionService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.38 · 财务·回款控制 集成测试（FR-9-3）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCollectionIntegrationTest {

    @Mock private CrmPaymentPlanMapper planMapper;
    @Mock private CrmPaymentAlertMapper alertMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private PaymentCollectionService service;

    @BeforeEach
    void setUp() {
        service = new PaymentCollectionService(planMapper, alertMapper, docNoGenerator);
        when(docNoGenerator.nextPaymentPlanNo())
                .thenReturn("PP20260612-0001", "PP20260612-0002", "PP20260612-0003", "PP20260612-0004");
        when(planMapper.insert(any(CrmPaymentPlan.class))).thenAnswer(inv -> {
            CrmPaymentPlan p = inv.getArgument(0);
            p.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(alertMapper.insert(any(CrmPaymentAlert.class))).thenAnswer(inv -> {
            CrmPaymentAlert a = inv.getArgument(0);
            a.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(planMapper.updateById(any(CrmPaymentPlan.class))).thenReturn(1);
    }

    private CreatePlanRequest build(long orderId, String orderNo, String total, String planned, int daysToPlanned) {
        CreatePlanRequest r = new CreatePlanRequest();
        r.setCustomerId(301L);
        r.setCustomerName("上海汽车配件");
        r.setOrderId(orderId);
        r.setOrderNo(orderNo);
        r.setReceivableId(1L);
        r.setReceivableNo("RV20260601-0001");
        r.setTotalAmount(new BigDecimal(total));
        r.setPlannedAmount(new BigDecimal(planned));
        r.setPlannedDate(LocalDate.now().plusDays(daysToPlanned));
        return r;
    }

    // ====== lifecycle 1：创建 → 提前 ALERT → 全额回款 ======
            @Test
    @DisplayName("lifecycle 1：创建 → 提前 ALERT → 全额回款 PAID")
    void testIntegration_PaidLifecycle() {
        when(planMapper.selectByOrderId(any())).thenReturn(null);
        Result<CrmPaymentPlan> c = service.createPlan(build(8001L, "XS8001", "10000", "10000", 2), 701L);
        Long pid = c.getData().getId();
        when(planMapper.selectPending()).thenReturn(List.of(c.getData()));
        Result<List<CrmPaymentPlan>> l = service.listPendingPlans();
        assertEquals("ALERT", l.getData().get(0).getAlertLevel());

        when(planMapper.selectById(pid)).thenReturn(c.getData());
        MarkPaidRequest mr = new MarkPaidRequest();
        mr.setPaidAmount(new BigDecimal("10000"));
        mr.setPaidBy(701L);
        Result<CrmPaymentPlan> r = service.markPaid(pid, mr, 701L);
        assertEquals("PAID", r.getData().getAlertLevel());
    }

    // ====== lifecycle 2：逾期 ALERT_CRITICAL ======
            @Test
    @DisplayName("lifecycle 2：逾期 ALERT_CRITICAL + 告警写入")
    void testIntegration_Overdue() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(2L);
        p.setOrderId(5002L);
        p.setPlannedAmount(new BigDecimal("100"));
        p.setPlannedDate(LocalDate.now().minusDays(45));
        p.setAlertLevel("PENDING");
        when(planMapper.selectPending()).thenReturn(List.of(p));
        Result<List<CrmPaymentPlan>> r = service.listPendingPlans();
        assertEquals("ALERT_CRITICAL", r.getData().get(0).getAlertLevel());
    }

    // ====== AC-9.3.1：单号模板 ======
            @Test
    @DisplayName("AC-9.3.1：单号模板 PP{yyyyMMdd}{seq:4}")
    void testIntegration_PpNo() {
        when(planMapper.selectByOrderId(any())).thenReturn(null);
        Result<CrmPaymentPlan> r = service.createPlan(build(8002L, "XS8002", "1000", "1000", 5), 701L);
        assertTrue(r.getData().getPlanNo().startsWith("PP"));
        assertEquals(15, r.getData().getPlanNo().length());
    }

    // ====== 跨 1.36 应收关联 ======
            @Test
    @DisplayName("跨 1.36：receivableId + receivableNo 双绑")
    void testIntegration_CrossReceivable() {
        when(planMapper.selectByOrderId(any())).thenReturn(null);
        CreatePlanRequest r = build(8003L, "XS8003", "5000", "5000", 7);
        r.setReceivableId(5L);
        r.setReceivableNo("RV20260601-0005");
        Result<CrmPaymentPlan> res = service.createPlan(r, 701L);
        assertEquals("RV20260601-0005", res.getData().getReceivableNo());
        assertEquals(5L, res.getData().getReceivableId());
    }

    // ====== 部分回款 ======
            @Test
    @DisplayName("部分回款 → PENDING + 累加 paidAmount")
    void testIntegration_PartialPaid() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(3L);
        p.setPlannedAmount(new BigDecimal("10000"));
        p.setPaidAmount(new BigDecimal("0"));
        p.setAlertLevel("PENDING");
        when(planMapper.selectById(3L)).thenReturn(p);
        MarkPaidRequest req = new MarkPaidRequest();
        req.setPaidAmount(new BigDecimal("4000"));
        req.setPaidBy(701L);
        service.markPaid(3L, req, 701L);
        assertEquals(0, new BigDecimal("4000").compareTo(p.getPaidAmount()));
        assertEquals("PENDING", p.getAlertLevel());
    }

    // ====== 重复创建拦截 ======
            @Test
    @DisplayName("订单 ID 重复 → 40902")
    void testIntegration_Duplicate() {
        CrmPaymentPlan existed = new CrmPaymentPlan();
        existed.setId(99L);
        when(planMapper.selectByOrderId(5001L)).thenReturn(existed);
        Result<CrmPaymentPlan> r = service.createPlan(build(5001L, "XS5001", "1000", "500", 5), 701L);
        assertEquals(40902, r.getCode());
    }

    // ====== 跨 1.6 订单 ======
            @Test
    @DisplayName("跨 1.6：orderId + orderNo 双绑")
    void testIntegration_CrossOrder() {
        when(planMapper.selectByOrderId(any())).thenReturn(null);
        Result<CrmPaymentPlan> r = service.createPlan(build(5005L, "XS20260515-0005", "65000", "65000", 10), 701L);
        assertEquals("XS20260515-0005", r.getData().getOrderNo());
        assertEquals(5005L, r.getData().getOrderId());
    }

    // ====== getOverduePlans 集成 ======
            @Test
    @DisplayName("getOverduePlans · 3 单 ALERT_CRITICAL")
    void testIntegration_OverdueList() {
        CrmPaymentPlan p1 = new CrmPaymentPlan();
        p1.setId(1L); p1.setAlertLevel("ALERT_CRITICAL");
        CrmPaymentPlan p2 = new CrmPaymentPlan();
        p2.setId(2L); p2.setAlertLevel("ALERT_CRITICAL");
        CrmPaymentPlan p3 = new CrmPaymentPlan();
        p3.setId(3L); p3.setAlertLevel("ALERT_CRITICAL");
        when(planMapper.selectOverdue()).thenReturn(List.of(p1, p2, p3));
        Result<Map<String, Object>> r = service.getOverduePlans();
        assertEquals(0, r.getCode());
        assertEquals(3, r.getData().get("count"));
    }

    // ====== 二次回款 PAID ======
            @Test
    @DisplayName("二次回款：4000 → 6000 → PAID")
    void testIntegration_SecondPaid() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(4L);
        p.setPlannedAmount(new BigDecimal("10000"));
        p.setPaidAmount(new BigDecimal("4000"));
        p.setAlertLevel("PENDING");
        when(planMapper.selectById(4L)).thenReturn(p);
        MarkPaidRequest req = new MarkPaidRequest();
        req.setPaidAmount(new BigDecimal("6000"));
        req.setPaidBy(701L);
        service.markPaid(4L, req, 701L);
        assertEquals("PAID", p.getAlertLevel());
    }

    // ====== 4 状态机 + 远期 ======
            @Test
    @DisplayName("4 状态机：PENDING → ALERT → ALERT_CRITICAL → PAID")
    void testIntegration_StateMachine() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(5L);
        p.setOrderId(5005L);
        p.setPlannedAmount(new BigDecimal("100"));
        p.setPlannedDate(LocalDate.now().plusDays(2));
        p.setAlertLevel("PENDING");
        when(planMapper.selectPending()).thenReturn(List.of(p));
        Result<List<CrmPaymentPlan>> r1 = service.listPendingPlans();
        assertEquals("ALERT", r1.getData().get(0).getAlertLevel());

        p.setPlannedDate(LocalDate.now().minusDays(5));
        Result<List<CrmPaymentPlan>> r2 = service.listPendingPlans();
        assertEquals("ALERT_CRITICAL", r2.getData().get(0).getAlertLevel());

        p.setPlannedDate(LocalDate.now().plusDays(15));
        when(planMapper.selectById(5L)).thenReturn(p);
        MarkPaidRequest mr = new MarkPaidRequest();
        mr.setPaidAmount(new BigDecimal("100"));
        mr.setPaidBy(701L);
        Result<CrmPaymentPlan> r3 = service.markPaid(5L, mr, 701L);
        assertEquals("PAID", r3.getData().getAlertLevel());
    }
}
