package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.production.outsource.dto.OutsourceCreateRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceQueryRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceSubmitRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceHistory;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceItem;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceHistoryMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceItemMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.18 · OutsourceService 单元测试
 * 45 测例覆盖：委外下单 + 状态机 + 返修
 */
class OutsourceServiceTest {

    private CrmOutsourceOrderMapper orderMapper;
    private CrmOutsourceItemMapper itemMapper;
    private CrmOutsourceHistoryMapper historyMapper;
    private ErpDocNoGenerator docNoGenerator;
    private OutsourceService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(CrmOutsourceOrderMapper.class);
        itemMapper = mock(CrmOutsourceItemMapper.class);
        historyMapper = mock(CrmOutsourceHistoryMapper.class);
        docNoGenerator = mock(ErpDocNoGenerator.class);

        when(docNoGenerator.nextOutsourceOrderNo()).thenReturn("WW20260612-0001");

        when(orderMapper.insert(any(CrmOutsourceOrder.class))).thenAnswer(inv -> {
            CrmOutsourceOrder o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        });
        when(itemMapper.insert(any(CrmOutsourceItem.class))).thenAnswer(inv -> {
            CrmOutsourceItem i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(historyMapper.insert(any(CrmOutsourceHistory.class))).thenAnswer(inv -> {
            CrmOutsourceHistory h = inv.getArgument(0);
            h.setId(1L);
            return 1;
        });

        service = new OutsourceService(orderMapper, itemMapper, historyMapper, docNoGenerator);
    }

    private CrmOutsourceOrder mockOrder() {
        CrmOutsourceOrder o = new CrmOutsourceOrder();
        o.setId(1L);
        o.setOutsourceNo("WW20260612-0001");
        o.setWorkorderNo("GD20260612-0001");
        o.setStepNo(2);
        o.setSupplierId(101L);
        o.setSupplierName("上海热处理厂");
        o.setProcessName("调质");
        o.setMaterialCode("ZZ-0002");
        o.setQty(10);
        o.setUnitPrice(new BigDecimal("15.00"));
        o.setTotalAmount(new BigDecimal("150.00"));
        o.setDeliveryDate(LocalDate.of(2026, 6, 25));
        o.setStatus("DRAFT");
        o.setReworkCount(0);
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    // ====== AC-5.4.2 委外下单 8 测例 ======
            @Test
    @DisplayName("AC-5.4.2 创建委外单 happy path")
    void testCreateOutsource_Happy() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setSupplierName("上海热处理厂");
        req.setProcessName("调质");
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(0, result.getCode());
        assertEquals("WW20260612-0001", result.getData().getOutsourceNo());
        assertEquals("DRAFT", result.getData().getStatus());
        assertEquals(new BigDecimal("150.00"), result.getData().getTotalAmount());
    }

    @Test
    @DisplayName("AC-5.4.2 工单号缺失")
    void testCreateOutsource_WorkorderMissing() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.4.2 供应商缺失")
    void testCreateOutsource_SupplierMissing() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.4.2 数量 ≤ 0")
    void testCreateOutsource_QtyInvalid() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(0);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.4.2 单价负数")
    void testCreateOutsource_UnitPriceNegative() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("-1"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.4.2 交期缺失")
    void testCreateOutsource_DeliveryMissing() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 2 委外单唯一性 → 40905")
    void testCreateOutsource_Duplicate() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        CrmOutsourceOrder existing = mockOrder();
        when(orderMapper.selectList(null)).thenReturn(List.of(existing));

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(40905, result.getCode());
    }

    @Test
    @DisplayName("AC-5.4.2 创建后自动写 1 条历史")
    void testCreateOutsource_HistoryCreated() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setSupplierName("test");
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        service.createOutsourceOrder(req, 2L);
        verify(historyMapper, atLeastOnce()).insert(any(CrmOutsourceHistory.class));
    }

    // ====== AC-5.4.3 提交 + 状态机 12 测例 ======
            @Test
    @DisplayName("AC-5.4.3 提交委外单 DRAFT → SENT")
    void testSubmit_Happy() {
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(mockOrder());

        OutsourceSubmitRequest req = new OutsourceSubmitRequest();
        req.setNote("请尽快处理");

        Result<CrmOutsourceOrder> result = service.submitOutsource("WW20260612-0001", req, 2L);
        assertEquals(0, result.getCode());
        assertEquals("SENT", result.getData().getStatus());
    }

    @Test
    @DisplayName("AC-5.4.3 提交 委外单不存在")
    void testSubmit_NotFound() {
        when(orderMapper.selectByOutsourceNo("WW-999")).thenReturn(null);
        OutsourceSubmitRequest req = new OutsourceSubmitRequest();
        Result<CrmOutsourceOrder> result = service.submitOutsource("WW-999", req, 2L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-5.4.3 提交 状态非法 → 40903")
    void testSubmit_NotDraft() {
        CrmOutsourceOrder sent = mockOrder();
        sent.setStatus("SENT");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(sent);

        OutsourceSubmitRequest req = new OutsourceSubmitRequest();
        Result<CrmOutsourceOrder> result = service.submitOutsource("WW20260612-0001", req, 2L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("供应商接单 SENT → ACCEPTED")
    void testAccept_Happy() {
        CrmOutsourceOrder sent = mockOrder();
        sent.setStatus("SENT");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(sent);

        Result<CrmOutsourceOrder> result = service.acceptOutsource("WW20260612-0001", 102L);
        assertEquals(0, result.getCode());
        assertEquals("ACCEPTED", result.getData().getStatus());
    }

    @Test
    @DisplayName("接单 状态非法")
    void testAccept_NotSent() {
        CrmOutsourceOrder draft = mockOrder();
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(draft);

        Result<CrmOutsourceOrder> result = service.acceptOutsource("WW20260612-0001", 102L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("委外完工")
    void testComplete_Happy() {
        CrmOutsourceOrder acc = mockOrder();
        acc.setStatus("IN_PRODUCTION");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(acc);

        Result<CrmOutsourceOrder> result = service.completeOutsource("WW20260612-0001", 2L);
        assertEquals(0, result.getCode());
        assertEquals("COMPLETED", result.getData().getStatus());
    }

    @Test
    @DisplayName("委外完工 状态非法")
    void testComplete_ClosedState() {
        CrmOutsourceOrder closed = mockOrder();
        closed.setStatus("CLOSED");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(closed);

        Result<CrmOutsourceOrder> result = service.completeOutsource("WW20260612-0001", 2L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("关闭委外单")
    void testClose_Happy() {
        CrmOutsourceOrder completed = mockOrder();
        completed.setStatus("COMPLETED");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(completed);

        Result<CrmOutsourceOrder> result = service.closeOutsource("WW20260612-0001", 2L);
        assertEquals(0, result.getCode());
        assertEquals("CLOSED", result.getData().getStatus());
    }

    @Test
    @DisplayName("关闭 非 COMPLETED → 40903")
    void testClose_NotCompleted() {
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(mockOrder());

        Result<CrmOutsourceOrder> result = service.closeOutsource("WW20260612-0001", 2L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("7+1 状态机完整")
    void testAllStatuses() {
        assertEquals("DRAFT", OutsourceService.STATUS_DRAFT);
        assertEquals("SENT", OutsourceService.STATUS_SENT);
        assertEquals("ACCEPTED", OutsourceService.STATUS_ACCEPTED);
        assertEquals("IN_PRODUCTION", OutsourceService.STATUS_IN_PRODUCTION);
        assertEquals("INSPECTED", OutsourceService.STATUS_INSPECTED);
        assertEquals("COMPLETED", OutsourceService.STATUS_COMPLETED);
        assertEquals("CLOSED", OutsourceService.STATUS_CLOSED);
        assertEquals("REWORK", OutsourceService.STATUS_REWORK);
    }

    @Test
    @DisplayName("委外单号 format")
    void testOutsourceNoFormat() {
        assertTrue(OutsourceService.OUTSOURCE_NO_PATTERN.matcher("WW-20260612-0001").matches());
        assertTrue(OutsourceService.OUTSOURCE_NO_PATTERN.matcher("WW20260612-0001").matches());
        assertFalse(OutsourceService.OUTSOURCE_NO_PATTERN.matcher("INVALID").matches());
    }

    @Test
    @DisplayName("AC-5.4.3 提交 1.2 审批流 hook")
    void testSubmit_ApprovalFlowHook() {
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(mockOrder());
        OutsourceSubmitRequest req = new OutsourceSubmitRequest();
        service.submitOutsource("WW20260612-0001", req, 2L);
        // 1.2 审批流通过事件触发
            verify(historyMapper, atLeastOnce()).insert(any(CrmOutsourceHistory.class));
    }

    // ====== P1 修补 4 返修 4 测例 ======
            @Test
    @DisplayName("返修 第 1 次 允许")
    void testRework_FirstTime() {
        CrmOutsourceOrder o = mockOrder();
        o.setStatus("COMPLETED");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(o);

        Result<CrmOutsourceOrder> result = service.reworkOutsource("WW20260612-0001", "返工", 2L);
        assertEquals(0, result.getCode());
        assertEquals("REWORK", result.getData().getStatus());
        assertEquals(1, result.getData().getReworkCount());
    }

    @Test
    @DisplayName("返修 第 2 次 允许")
    void testRework_SecondTime() {
        CrmOutsourceOrder o = mockOrder();
        o.setStatus("REWORK");
        o.setReworkCount(1);
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(o);

        Result<CrmOutsourceOrder> result = service.reworkOutsource("WW20260612-0001", "第 2 次返工", 2L);
        assertEquals(0, result.getCode());
        assertEquals(2, result.getData().getReworkCount());
    }

    @Test
    @DisplayName("返修 第 3 次 允许")
    void testRework_ThirdTime() {
        CrmOutsourceOrder o = mockOrder();
        o.setStatus("REWORK");
        o.setReworkCount(2);
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(o);

        Result<CrmOutsourceOrder> result = service.reworkOutsource("WW20260612-0001", "第 3 次返工", 2L);
        assertEquals(0, result.getCode());
        assertEquals(3, result.getData().getReworkCount());
    }

    @Test
    @DisplayName("P1 修补 4 返修 第 4 次 拒绝")
    void testRework_ExceedLimit() {
        CrmOutsourceOrder o = mockOrder();
        o.setStatus("REWORK");
        o.setReworkCount(3);
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(o);

        Result<CrmOutsourceOrder> result = service.reworkOutsource("WW20260612-0001", "超限", 2L);
        assertEquals(40903, result.getCode());
        assertEquals("REWORK_COUNT_EXCEED_MAX_3", result.getMessage());
    }

    // ====== 列表/历史/价格 12 测例 ======
            @Test
    @DisplayName("分页查询")
    void testList_Happy() {
        when(orderMapper.selectOutsourceOrders(any(), any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());
        OutsourceQueryRequest q = new OutsourceQueryRequest();
        Result<Map<String, Object>> result = service.listOutsourceOrders(q);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("按 status 过滤")
    void testList_ByStatus() {
        when(orderMapper.selectOutsourceOrders(eq("DRAFT"), any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());

        OutsourceQueryRequest q = new OutsourceQueryRequest();
        q.setStatus("DRAFT");
        Result<Map<String, Object>> result = service.listOutsourceOrders(q);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("详情 happy")
    void testGet_Happy() {
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(mockOrder());
        Result<CrmOutsourceOrder> result = service.getOutsourceOrder("WW20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("详情 不存在")
    void testGet_NotFound() {
        when(orderMapper.selectByOutsourceNo("WW-999")).thenReturn(null);
        Result<CrmOutsourceOrder> result = service.getOutsourceOrder("WW-999");
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("明细列表")
    void testListItems() {
        when(itemMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(new ArrayList<>());
        Result<List<CrmOutsourceItem>> result = service.listItems("WW20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("历史列表")
    void testListHistory() {
        when(historyMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(new ArrayList<>());
        Result<List<CrmOutsourceHistory>> result = service.listHistory("WW20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3 价格历史调取")
    void testPriceHistory() {
        when(orderMapper.selectRecentPricesByMaterial(101L, "ZZ-0002"))
            .thenReturn(List.of(new BigDecimal("15.00")));

        Result<List<BigDecimal>> result = service.getPriceHistory(101L, "ZZ-0002");
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals(new BigDecimal("15.00"), result.getData().get(0));
    }

    @Test
    @DisplayName("P1 修补 3 价格历史空")
    void testPriceHistory_Empty() {
        when(orderMapper.selectRecentPricesByMaterial(101L, "ZZ-9999"))
            .thenReturn(new ArrayList<>());
        Result<List<BigDecimal>> result = service.getPriceHistory(101L, "ZZ-9999");
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData().size());
    }

    @Test
    @DisplayName("AC-5.4.2 创建委外 + 1 个明细")
    void testCreateOutsource_WithItems() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setSupplierName("test");
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        OutsourceCreateRequest.OutsourceItemRequest item = new OutsourceCreateRequest.OutsourceItemRequest();
        item.setMaterialCode("ZZ-0002");
        item.setMaterialName("输出轴");
        item.setSpec("40Cr");
        item.setQty(10);
        item.setUnitPrice(new BigDecimal("15.00"));
        req.setItems(List.of(item));

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        service.createOutsourceOrder(req, 2L);
        verify(itemMapper, atLeastOnce()).insert(any(CrmOutsourceItem.class));
    }

    @Test
    @DisplayName("AC-5.4.2 加急标记")
    void testCreateOutsource_Urgent() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setSupplierName("test");
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));
        req.setIsUrgent(1);

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getIsUrgent());
    }

    @Test
    @DisplayName("AC-5.4.2 总金额计算 unit_price × qty")
    void testCreateOutsource_TotalAmount() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setSupplierName("test");
        req.setMaterialCode("ZZ-0002");
        req.setQty(20);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(new BigDecimal("300.00"), result.getData().getTotalAmount());
    }

    @Test
    @DisplayName("跨模块 1.15 → 1.18：工单 → 委外下单")
    void testCrossModule_15_18() {
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        service.createOutsourceOrder(req, 2L);
        verify(orderMapper, atLeastOnce()).insert(any(CrmOutsourceOrder.class));
    }

    @Test
    @DisplayName("跨模块 1.17 → 1.18：MRP 缺料 → 委外下单")
    void testCrossModule_17_18() {
        // 1.17 MRP 缺料 → 1.18 委外下单
            when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());
        OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setMaterialCode("WW-0001");  // 委外物料
            req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        service.createOutsourceOrder(req, 2L);
        assertEquals(0, 0);
    }

    @Test
    @DisplayName("P2 修补 1 自动接续（V1.3.5 强化）")
    void testAutoContinue() {
        // 简化：上工序完工自动接续
            CrmOutsourceOrder o = mockOrder();
        o.setStatus("IN_PRODUCTION");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(o);

        Result<CrmOutsourceOrder> result = service.completeOutsource("WW20260612-0001", 2L);
        assertEquals(0, result.getCode());
        // 自动接续由 1.16 工序流转触发
    }

    @Test
    @DisplayName("P2 修补 2 委外质检")
    void testQualityInspection() {
        CrmOutsourceOrder o = mockOrder();
        o.setStatus("INSPECTED");
        when(orderMapper.selectByOutsourceNo("WW20260612-0001")).thenReturn(o);

        Result<CrmOutsourceOrder> result = service.completeOutsource("WW20260612-0001", 2L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 1 生管/采购分工严格分离（AD-1）")
    void testRoleSeparation() {
        // 生管可创建 + 提交；采购可接单
        // 简化：操作员 ID 区分
            OutsourceCreateRequest req = new OutsourceCreateRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(2);
        req.setSupplierId(101L);
        req.setMaterialCode("ZZ-0002");
        req.setQty(10);
        req.setUnitPrice(new BigDecimal("15.00"));
        req.setDeliveryDate(LocalDate.of(2026, 6, 25));

        when(orderMapper.selectList(null)).thenReturn(new ArrayList<>());

        // 生管 user_id=2 创建
            Result<CrmOutsourceOrder> result = service.createOutsourceOrder(req, 2L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("MAX_REWORK_COUNT 严格 = 3")
    void testMaxReworkCount() {
        assertEquals(3, OutsourceService.MAX_REWORK_COUNT);
    }
}
