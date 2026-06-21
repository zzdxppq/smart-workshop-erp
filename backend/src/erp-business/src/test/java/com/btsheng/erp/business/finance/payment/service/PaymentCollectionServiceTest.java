package com.btsheng.erp.business.finance.payment.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.payment.dto.CreatePlanRequest;
import com.btsheng.erp.business.finance.payment.dto.MarkPaidRequest;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentAlert;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentPlan;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentAlertMapper;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentPlanMapper;
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
 * V1.3.7 · Story 1.38 · 财务·回款控制 Service 单元测试（FR-9-3）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCollectionServiceTest {

    @Mock private CrmPaymentPlanMapper planMapper;
    @Mock private CrmPaymentAlertMapper alertMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private PaymentCollectionService service;

    @BeforeEach
    void setUp() {
        service = new PaymentCollectionService(planMapper, alertMapper, docNoGenerator);
        when(docNoGenerator.nextPaymentPlanNo())
                .thenReturn("PP20260612-0001", "PP20260612-0002", "PP20260612-0003");
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

    private CreatePlanRequest buildValid() {
        CreatePlanRequest r = new CreatePlanRequest();
        r.setCustomerId(301L);
        r.setCustomerName("上海汽车配件");
        r.setOrderId(5001L);
        r.setOrderNo("XS20260501-0001");
        r.setReceivableId(1L);
        r.setReceivableNo("RV20260601-0001");
        r.setTotalAmount(new BigDecimal("120000"));
        r.setPlannedAmount(new BigDecimal("120000"));
        r.setPlannedDate(LocalDate.now().plusDays(10));
        return r;
    }

    // ====== createPlan 6 测例 ======
            @Test
    @DisplayName("createPlan happy path · 单号 PP 前缀")
    void testCreate_OK() {
        when(planMapper.selectByOrderId(any())).thenReturn(null);
        Result<CrmPaymentPlan> r = service.createPlan(buildValid(), 701L);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().getPlanNo().startsWith("PP"));
        assertEquals("PENDING", r.getData().getAlertLevel());
    }

    @Test
    @DisplayName("P1 修补 1：plannedAmount > totalAmount → 40909")
    void testCreate_PlannedExceed() {
        CreatePlanRequest r = buildValid();
        r.setPlannedAmount(new BigDecimal("150000"));
        Result<CrmPaymentPlan> res = service.createPlan(r, 701L);
        assertEquals(40909, res.getCode());
        assertEquals("PLANNED_EXCEED_ORDER", res.getMessage());
    }

    @Test
    @DisplayName("订单 ID 重复 → 40902")
    void testCreate_Duplicate() {
        CrmPaymentPlan existed = new CrmPaymentPlan();
        existed.setId(99L);
        when(planMapper.selectByOrderId(5001L)).thenReturn(existed);
        Result<CrmPaymentPlan> r = service.createPlan(buildValid(), 701L);
        assertEquals(40902, r.getCode());
    }

    @Test
    @DisplayName("缺订单号 → 40001")
    void testCreate_NoOrder() {
        CreatePlanRequest r = buildValid();
        r.setOrderId(null);
        Result<CrmPaymentPlan> res = service.createPlan(r, 701L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("计划金额 0 → 40001")
    void testCreate_PlannedZero() {
        CreatePlanRequest r = buildValid();
        r.setPlannedAmount(BigDecimal.ZERO);
        Result<CrmPaymentPlan> res = service.createPlan(r, 701L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺计划日 → 40001")
    void testCreate_NoDate() {
        CreatePlanRequest r = buildValid();
        r.setPlannedDate(null);
        Result<CrmPaymentPlan> res = service.createPlan(r, 701L);
        assertEquals(40001, res.getCode());
    }

    // ====== listPendingPlans 4 测例 ======
            @Test
    @DisplayName("P1 修补 2：提前 2 天 → ALERT + 写告警")
    void testList_Alert() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(1L);
        p.setPlanNo("PP20260612-0001");
        p.setOrderId(5001L);
        p.setPlannedAmount(new BigDecimal("100"));
        p.setPlannedDate(LocalDate.now().plusDays(2));
        p.setAlertLevel("PENDING");
        when(planMapper.selectPending()).thenReturn(List.of(p));
        Result<List<CrmPaymentPlan>> r = service.listPendingPlans();
        assertEquals("ALERT", r.getData().get(0).getAlertLevel());
    }

    @Test
    @DisplayName("P1 修补 3：逾期 → ALERT_CRITICAL + 写告警")
    void testList_Overdue() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(2L);
        p.setPlanNo("PP20260612-0002");
        p.setOrderId(5002L);
        p.setPlannedAmount(new BigDecimal("100"));
        p.setPlannedDate(LocalDate.now().minusDays(10));
        p.setAlertLevel("PENDING");
        when(planMapper.selectPending()).thenReturn(List.of(p));
        Result<List<CrmPaymentPlan>> r = service.listPendingPlans();
        assertEquals("ALERT_CRITICAL", r.getData().get(0).getAlertLevel());
    }

    @Test
    @DisplayName("远期 > 3 天 → 保持 PENDING")
    void testList_FarFuture() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(3L);
        p.setOrderId(5003L);
        p.setPlannedAmount(new BigDecimal("100"));
        p.setPlannedDate(LocalDate.now().plusDays(15));
        p.setAlertLevel("PENDING");
        when(planMapper.selectPending()).thenReturn(List.of(p));
        Result<List<CrmPaymentPlan>> r = service.listPendingPlans();
        assertEquals("PENDING", r.getData().get(0).getAlertLevel());
    }

    @Test
    @DisplayName("listPendingPlans · 空返回空列表")
    void testList_Empty() {
        when(planMapper.selectPending()).thenReturn(new ArrayList<>());
        Result<List<CrmPaymentPlan>> r = service.listPendingPlans();
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().size());
    }

    // ====== markPaid 3 测例 ======
            @Test
    @DisplayName("markPaid 全额回款 → PAID")
    void testPaid_Full() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(1L);
        p.setPlannedAmount(new BigDecimal("120000"));
        p.setPaidAmount(BigDecimal.ZERO);
        p.setAlertLevel("PENDING");
        when(planMapper.selectById(1L)).thenReturn(p);
        MarkPaidRequest req = new MarkPaidRequest();
        req.setPaidAmount(new BigDecimal("120000"));
        req.setPaidBy(701L);
        Result<CrmPaymentPlan> r = service.markPaid(1L, req, 701L);
        assertEquals(0, r.getCode());
        assertEquals("PAID", p.getAlertLevel());
    }

    @Test
    @DisplayName("P1 修补 1：本次回款 > 剩余未回款 → 40909")
    void testPaid_Exceed() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(1L);
        p.setPlannedAmount(new BigDecimal("1000"));
        p.setPaidAmount(new BigDecimal("400"));
        p.setAlertLevel("PARTIAL");
        when(planMapper.selectById(1L)).thenReturn(p);
        MarkPaidRequest req = new MarkPaidRequest();
        req.setPaidAmount(new BigDecimal("800"));
        req.setPaidBy(701L);
        Result<CrmPaymentPlan> r = service.markPaid(1L, req, 701L);
        assertEquals(40909, r.getCode());
    }

    @Test
    @DisplayName("markPaid 已 PAID → 40903")
    void testPaid_AlreadyPaid() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(1L);
        p.setAlertLevel("PAID");
        when(planMapper.selectById(1L)).thenReturn(p);
        MarkPaidRequest req = new MarkPaidRequest();
        req.setPaidAmount(new BigDecimal("100"));
        Result<CrmPaymentPlan> r = service.markPaid(1L, req, 701L);
        assertEquals(40903, r.getCode());
    }

    // ====== getOverduePlans 1 测例 ======
            @Test
    @DisplayName("getOverduePlans · ALERT_CRITICAL 列表")
    void testOverdue() {
        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setId(1L);
        p.setAlertLevel("ALERT_CRITICAL");
        when(planMapper.selectOverdue()).thenReturn(List.of(p));
        Result<Map<String, Object>> r = service.getOverduePlans();
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().get("count"));
    }
}
