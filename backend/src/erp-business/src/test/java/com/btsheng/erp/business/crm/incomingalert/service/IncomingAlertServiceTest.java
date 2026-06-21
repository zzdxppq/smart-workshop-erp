package com.btsheng.erp.business.crm.incomingalert.service;

import com.btsheng.erp.business.crm.incomingalert.dto.CreateAlertRequest;
import com.btsheng.erp.business.crm.incomingalert.dto.MarkArrivedRequest;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncoming;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncomingAlert;
import com.btsheng.erp.business.crm.incomingalert.mapper.CrmIncomingAlertMapper;
import com.btsheng.erp.business.crm.incomingalert.mapper.CrmIncomingMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
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
 * V1.3.7 · Story 1.34 · 采购·到货提醒 Service 单元测试（FR-8-3）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncomingAlertServiceTest {

    @Mock private CrmIncomingAlertMapper alertMapper;
    @Mock private CrmIncomingMapper incomingMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private IncomingAlertService service;

    @BeforeEach
    void setUp() {
        service = new IncomingAlertService(alertMapper, incomingMapper, docNoGenerator);
        when(docNoGenerator.nextIncomingAlertNo())
                .thenReturn("IA20260612-0001", "IA20260612-0002", "IA20260612-0003");
        when(alertMapper.insert(any(CrmIncomingAlert.class))).thenAnswer(inv -> {
            CrmIncomingAlert a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });
        when(incomingMapper.insert(any(CrmIncoming.class))).thenAnswer(inv -> {
            CrmIncoming i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(alertMapper.updateById(any(CrmIncomingAlert.class))).thenReturn(1);
    }

    private CreateAlertRequest buildValid() {
        CreateAlertRequest r = new CreateAlertRequest();
        r.setPoId(1L);
        r.setPoNo("PO20260401-0001");
        r.setVendorId(901L);
        r.setVendorName("上海铝业");
        r.setMaterialId(1001L);
        r.setMaterialCode("M-AL6061-PT");
        r.setQty(new BigDecimal("100"));
        r.setUnit("PCS");
        r.setExpectedDate(LocalDate.now().plusDays(7));
        return r;
    }

    // ====== createAlert 4 测例 ======
            @Test
    @DisplayName("createAlert happy path · 单号 IA 前缀")
    void testCreate_OK() {
        when(alertMapper.selectByPoAndMaterial(1L, 1001L)).thenReturn(null);
        Result<CrmIncomingAlert> r = service.createAlert(buildValid(), 503L);
        assertEquals(0, r.getCode());
        assertEquals("IA20260612-0001", r.getData().getAlertNo());
        assertEquals("PENDING", r.getData().getAlertLevel());
    }

    @Test
    @DisplayName("P1 修补 1：缺预估到货日 → 40001")
    void testCreate_ExpectedDateRequired() {
        CreateAlertRequest r = buildValid();
        r.setExpectedDate(null);
        Result<CrmIncomingAlert> result = service.createAlert(r, 503L);
        assertEquals(40001, result.getCode());
        assertEquals("EXPECTED_DATE_REQUIRED", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 4：(po_id, material_id) 重复 → 40902")
    void testCreate_Duplicate() {
        CrmIncomingAlert existed = new CrmIncomingAlert();
        existed.setId(99L);
        when(alertMapper.selectByPoAndMaterial(1L, 1001L)).thenReturn(existed);
        Result<CrmIncomingAlert> r = service.createAlert(buildValid(), 503L);
        assertEquals(40902, r.getCode());
    }

    @Test
    @DisplayName("createAlert 缺物料 → 40001")
    void testCreate_NoMaterial() {
        CreateAlertRequest r = buildValid();
        r.setMaterialId(null);
        Result<CrmIncomingAlert> result = service.createAlert(r, 503L);
        assertEquals(40001, result.getCode());
    }

    // ====== listPendingAlerts 3 测例 ======
            @Test
    @DisplayName("P1 修补 2：提前 3 天 → ALERT")
    void testList_Alert() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(1L);
        a.setPoId(1L);
        a.setMaterialId(1001L);
        a.setQty(new BigDecimal("100"));
        a.setExpectedDate(LocalDate.now().plusDays(2));
        a.setAlertLevel("PENDING");
        a.setRemindedCount(0);
        when(alertMapper.selectPendingAll()).thenReturn(List.of(a));

        Result<List<CrmIncomingAlert>> r = service.listPendingAlerts();
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
        assertEquals("ALERT", r.getData().get(0).getAlertLevel());
        assertEquals(1, r.getData().get(0).getRemindedCount());
    }

    @Test
    @DisplayName("P1 修补 3：逾期 → ALERT_CRITICAL")
    void testList_Overdue() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(2L);
        a.setPoId(2L);
        a.setMaterialId(1002L);
        a.setQty(new BigDecimal("50"));
        a.setExpectedDate(LocalDate.now().minusDays(2));
        a.setAlertLevel("PENDING");
        when(alertMapper.selectPendingAll()).thenReturn(List.of(a));

        Result<List<CrmIncomingAlert>> r = service.listPendingAlerts();
        assertEquals(0, r.getCode());
        assertEquals("ALERT_CRITICAL", r.getData().get(0).getAlertLevel());
    }

    @Test
    @DisplayName("远期 > 3 天 → 保持 PENDING")
    void testList_FarFuture() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(3L);
        a.setPoId(3L);
        a.setMaterialId(1003L);
        a.setQty(new BigDecimal("10"));
        a.setExpectedDate(LocalDate.now().plusDays(10));
        a.setAlertLevel("PENDING");
        when(alertMapper.selectPendingAll()).thenReturn(List.of(a));

        Result<List<CrmIncomingAlert>> r = service.listPendingAlerts();
        assertEquals(0, r.getCode());
        assertEquals("PENDING", r.getData().get(0).getAlertLevel());
    }

    // ====== markArrived 2 测例 ======
            @Test
    @DisplayName("markArrived 全部到货 → ARRIVED")
    void testMark_AllArrived() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(4L);
        a.setPoId(1L);
        a.setPoNo("PO20260401-0001");
        a.setMaterialId(1001L);
        a.setQty(new BigDecimal("100"));
        a.setAlertLevel("PENDING");
        when(alertMapper.selectById(4L)).thenReturn(a);

        MarkArrivedRequest req = new MarkArrivedRequest();
        req.setArrivedQty(new BigDecimal("100"));

        Result<Map<String, Object>> r = service.markArrived(4L, req, 503L);
        assertEquals(0, r.getCode());
        assertEquals("ARRIVED", a.getAlertLevel());
    }

    @Test
    @DisplayName("markArrived 部分到货 → 累加 arrivedQty")
    void testMark_Partial() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(5L);
        a.setPoId(1L);
        a.setPoNo("PO20260401-0001");
        a.setMaterialId(1001L);
        a.setQty(new BigDecimal("100"));
        a.setArrivedQty(new BigDecimal("30"));
        a.setAlertLevel("PENDING");
        when(alertMapper.selectById(5L)).thenReturn(a);

        MarkArrivedRequest req = new MarkArrivedRequest();
        req.setArrivedQty(new BigDecimal("20"));

        Result<Map<String, Object>> r = service.markArrived(5L, req, 503L);
        assertEquals(0, r.getCode());
        assertEquals(0, a.getArrivedQty().compareTo(new BigDecimal("50")));
    }

    // ====== getOverdueAlerts 1 测例 ======
            @Test
    @DisplayName("getOverdueAlerts 列出 ALERT_CRITICAL")
    void testOverdue() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(6L);
        a.setAlertLevel("ALERT_CRITICAL");
        when(alertMapper.selectByAlertLevel("ALERT_CRITICAL")).thenReturn(List.of(a));
        Result<List<CrmIncomingAlert>> r = service.getOverdueAlerts();
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
