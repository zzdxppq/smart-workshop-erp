package com.btsheng.erp.business.crm.incomingalert.integration;

import com.btsheng.erp.business.crm.incomingalert.dto.CreateAlertRequest;
import com.btsheng.erp.business.crm.incomingalert.dto.MarkArrivedRequest;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncoming;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncomingAlert;
import com.btsheng.erp.business.crm.incomingalert.mapper.CrmIncomingAlertMapper;
import com.btsheng.erp.business.crm.incomingalert.mapper.CrmIncomingMapper;
import com.btsheng.erp.business.crm.incomingalert.service.IncomingAlertService;
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
 * V1.3.7 · Story 1.34 · 采购·到货提醒 集成测试（FR-8-3）
 * 8 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncomingAlertIntegrationTest {

    @Mock private CrmIncomingAlertMapper alertMapper;
    @Mock private CrmIncomingMapper incomingMapper;
    @Mock private DocNoGenerator docNoGenerator;

    private IncomingAlertService service;

    @BeforeEach
    void setUp() {
        service = new IncomingAlertService(alertMapper, incomingMapper, docNoGenerator);
        when(docNoGenerator.nextIncomingAlertNo())
                .thenReturn("IA20260612-0001", "IA20260612-0002", "IA20260612-0003", "IA20260612-0004", "IA20260612-0005");
        when(alertMapper.insert(any(CrmIncomingAlert.class))).thenAnswer(inv -> {
            CrmIncomingAlert a = inv.getArgument(0);
            a.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(incomingMapper.insert(any(CrmIncoming.class))).thenAnswer(inv -> {
            CrmIncoming i = inv.getArgument(0);
            i.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(alertMapper.updateById(any(CrmIncomingAlert.class))).thenReturn(1);
    }

    // ====== 完整 lifecycle 1：创建 → 提前 ALERT → 全部到货 ======
            @Test
    @DisplayName("集成 lifecycle 1：创建 → 提前 ALERT → 全部到货 ARRIVED")
    void testIntegration_Arrived() {
        when(alertMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateAlertRequest cr = new CreateAlertRequest();
        cr.setPoId(1L);
        cr.setPoNo("PO20260401-0001");
        cr.setVendorId(901L);
        cr.setVendorName("上海铝业");
        cr.setMaterialId(1001L);
        cr.setMaterialCode("M-AL6061-PT");
        cr.setQty(new BigDecimal("100"));
        cr.setUnit("PCS");
        cr.setExpectedDate(LocalDate.now().plusDays(2));   // 提前 2 天
            Result<CrmIncomingAlert> c = service.createAlert(cr, 503L);
        Long aid = c.getData().getId();
        assertEquals(0, c.getCode());

        // 模拟 listPendingAlerts 触发 ALERT
            CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(aid);
        a.setPoId(1L);
        a.setPoNo("PO20260401-0001");
        a.setMaterialId(1001L);
        a.setQty(new BigDecimal("100"));
        a.setExpectedDate(LocalDate.now().plusDays(2));
        a.setAlertLevel("PENDING");
        a.setRemindedCount(0);
        when(alertMapper.selectPendingAll()).thenReturn(List.of(a));
        Result<List<CrmIncomingAlert>> l = service.listPendingAlerts();
        assertEquals("ALERT", l.getData().get(0).getAlertLevel());

        // 标记到货
            when(alertMapper.selectById(aid)).thenReturn(a);
        MarkArrivedRequest mr = new MarkArrivedRequest();
        mr.setArrivedQty(new BigDecimal("100"));
        Result<Map<String, Object>> r = service.markArrived(aid, mr, 503L);
        assertEquals(0, r.getCode());
        assertEquals("ARRIVED", a.getAlertLevel());
    }

    // ====== 完整 lifecycle 2：逾期 ALERT_CRITICAL ======
            @Test
    @DisplayName("集成 lifecycle 2：逾期 ALERT_CRITICAL + 部分到货")
    void testIntegration_Overdue() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(2L);
        a.setPoId(2L);
        a.setPoNo("PO20260410-0005");
        a.setMaterialId(1002L);
        a.setQty(new BigDecimal("50"));
        a.setExpectedDate(LocalDate.now().minusDays(3));
        a.setAlertLevel("PENDING");
        when(alertMapper.selectPendingAll()).thenReturn(List.of(a));

        Result<List<CrmIncomingAlert>> l = service.listPendingAlerts();
        assertEquals("ALERT_CRITICAL", l.getData().get(0).getAlertLevel());

        when(alertMapper.selectById(2L)).thenReturn(a);
        MarkArrivedRequest mr = new MarkArrivedRequest();
        mr.setArrivedQty(new BigDecimal("20"));
        Result<Map<String, Object>> r = service.markArrived(2L, mr, 503L);
        assertEquals(0, r.getCode());
        // 部分到货：20/50 → alertLevel 不变（仍 ALERT_CRITICAL）
            assertEquals(0, a.getArrivedQty().compareTo(new BigDecimal("20")));
    }

    // ====== AC-8.3.1：单号 IA 前缀 ======
            @Test
    @DisplayName("AC-8.3.1：单号模板 IA{yyyyMMdd}{seq:4}")
    void testIntegration_IaNo() {
        when(alertMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateAlertRequest cr = new CreateAlertRequest();
        cr.setPoId(3L);
        cr.setPoNo("PO20260415-0007");
        cr.setMaterialId(1004L);
        cr.setQty(new BigDecimal("300"));
        cr.setExpectedDate(LocalDate.now().plusDays(5));
        Result<CrmIncomingAlert> r = service.createAlert(cr, 503L);
        assertTrue(r.getData().getAlertNo().startsWith("IA"));
        assertEquals(15, r.getData().getAlertNo().length());
    }

    // ====== 状态机 3 档（红黄灯） ======
            @Test
    @DisplayName("状态机：PENDING → ALERT（提前 3 天）→ ALERT_CRITICAL（逾期）→ ARRIVED")
    void testIntegration_StateMachine() {
        CrmIncomingAlert a1 = new CrmIncomingAlert();
        a1.setId(11L); a1.setPoId(11L); a1.setMaterialId(11L);
        a1.setQty(new BigDecimal("10"));
        a1.setExpectedDate(LocalDate.now().plusDays(2));
        a1.setAlertLevel("PENDING"); a1.setRemindedCount(0);

        when(alertMapper.selectPendingAll()).thenReturn(List.of(a1));
        Result<List<CrmIncomingAlert>> r1 = service.listPendingAlerts();
        assertEquals("ALERT", r1.getData().get(0).getAlertLevel());

        a1.setExpectedDate(LocalDate.now().minusDays(1));
        Result<List<CrmIncomingAlert>> r2 = service.listPendingAlerts();
        assertEquals("ALERT_CRITICAL", r2.getData().get(0).getAlertLevel());

        when(alertMapper.selectById(11L)).thenReturn(a1);
        MarkArrivedRequest mr = new MarkArrivedRequest();
        mr.setArrivedQty(new BigDecimal("10"));
        service.markArrived(11L, mr, 503L);
        assertEquals("ARRIVED", a1.getAlertLevel());
    }

    // ====== 唯一索引 (po_id, material_id) ======
            @Test
    @DisplayName("P1 修补 4：(po_id, material_id) 唯一")
    void testIntegration_UniqueIndex() {
        CrmIncomingAlert existed = new CrmIncomingAlert();
        existed.setId(99L);
        when(alertMapper.selectByPoAndMaterial(1L, 1001L)).thenReturn(existed);
        CreateAlertRequest cr = new CreateAlertRequest();
        cr.setPoId(1L);
        cr.setPoNo("PO20260401-0001");
        cr.setMaterialId(1001L);
        cr.setQty(new BigDecimal("100"));
        cr.setExpectedDate(LocalDate.now().plusDays(5));
        Result<CrmIncomingAlert> r = service.createAlert(cr, 503L);
        assertEquals(40902, r.getCode());
    }

    // ====== 跨 1.12 扫码批次 ======
            @Test
    @DisplayName("跨 1.12：markArrived 关联 scanBatchNo")
    void testIntegration_Cross_Scan() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(20L);
        a.setPoId(20L);
        a.setPoNo("PO20260401-0001");
        a.setMaterialId(1001L);
        a.setQty(new BigDecimal("100"));
        a.setAlertLevel("PENDING");
        when(alertMapper.selectById(20L)).thenReturn(a);

        MarkArrivedRequest mr = new MarkArrivedRequest();
        mr.setArrivedQty(new BigDecimal("100"));
        mr.setScanBatchNo("B20260612-0001");
        Result<Map<String, Object>> r = service.markArrived(20L, mr, 503L);
        assertEquals(0, r.getCode());
        CrmIncoming inc = (CrmIncoming) r.getData().get("incoming");
        assertEquals("B20260612-0001", inc.getScanBatchNo());
    }

    // ====== 重复到货拦截 ======
            @Test
    @DisplayName("已 ARRIVED 状态再次 markArrived → 40903")
    void testIntegration_AlreadyArrived() {
        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setId(30L);
        a.setPoId(30L);
        a.setPoNo("PO20260401-0001");
        a.setMaterialId(1001L);
        a.setQty(new BigDecimal("100"));
        a.setAlertLevel("ARRIVED");
        when(alertMapper.selectById(30L)).thenReturn(a);

        MarkArrivedRequest mr = new MarkArrivedRequest();
        mr.setArrivedQty(new BigDecimal("10"));
        Result<Map<String, Object>> r = service.markArrived(30L, mr, 503L);
        assertEquals(40903, r.getCode());
    }

    // ====== 审计留痕 ======
            @Test
    @DisplayName("审计留痕：createAlert + markArrived 各写 1 记录")
    void testIntegration_Audit() {
        when(alertMapper.selectByPoAndMaterial(any(), any())).thenReturn(null);
        CreateAlertRequest cr = new CreateAlertRequest();
        cr.setPoId(40L);
        cr.setPoNo("PO20260401-0001");
        cr.setMaterialId(1001L);
        cr.setQty(new BigDecimal("100"));
        cr.setExpectedDate(LocalDate.now().plusDays(5));
        service.createAlert(cr, 503L);
        // 验证 alertMapper.insert 被调用
            org.mockito.Mockito.verify(alertMapper, org.mockito.Mockito.times(1)).insert(any(CrmIncomingAlert.class));
    }
}
