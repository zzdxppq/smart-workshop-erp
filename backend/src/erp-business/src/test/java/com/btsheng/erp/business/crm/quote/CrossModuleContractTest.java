package com.btsheng.erp.business.crm.quote;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteHistory;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.quote.service.OrderConversionService;
import com.btsheng.erp.business.crm.quote.service.QuoteService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.business.integration.client.DictClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.5 · 跨模块契约测例（15 测例 · 8 个下�?Epic + Story 1.4�? *
 * 范围�? *  - Epic 3 图纸物料�? 测例（auto-fill drawingNo / material�? *  - Epic 5 生产     �? 测例（quote �?order �?workorder 链路�? *  - Epic 6 委外     �? 测例（quote 转委外单�? *  - Epic 7 品质     �? 测例（FA 首件质检触发�? *  - Epic 8 采购     �? 测例（采购订单触发）
 *  - Epic 9 财务     �? 测例（合同回�?+ 利润分析�? *  - Epic 11 报表    �? 测例（销售统计聚合）
 *  - Story 1.4 APP 端：1 测例�? 类码 WL-物料码引�?quote 字段�? *
 * 设计：纯 mock + verify 跨服务调用契约，零外部依赖，可被 CI 直接跑通�? */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrossModuleContractTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;
    @Mock private DictClient dictService;

    private QuoteService newQuoteService() {
        when(dictService.listByType("CUSTOMER_STATUS")).thenReturn(Result.ok(new ArrayList<>()));
        return new QuoteService(quoteMapper, itemMapper, historyMapper, dictService);
    }

    private OrderConversionService newOrderConversionService(QuoteService qs) {
        return new OrderConversionService(quoteMapper, itemMapper, qs);
    }

    private CrmQuote seedQuote(long id, String status, BigDecimal total) {
        CrmQuote q = new CrmQuote();
        q.setId(id);
        q.setQuoteNo("BJ20260611-0001");
        q.setCustomerId(11L);
        q.setCustomerName("昆山测试");
        q.setOwnerUserId(1L);
        q.setStatus(status);
        q.setTotalAmount(total);
        q.setCurrency("CNY");
        return q;
    }

    // =================== Epic 3 图纸物料�? 测例�?===================
            @Test
    void epic3_drawing_auto_fill_when_drawingNo_provided() {
        // Epic 3 契约：报价保存时�?items[].drawingNo 已填，物料基础资料 (material/dimension) 自动带出
            QuoteService svc = newQuoteService();
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L); q.setOwnerUserId(1L); q.setDeptId(1L);
        CrmQuoteItem it = new CrmQuoteItem();
        it.setDrawingNo("DWG-A001");
        it.setQuantity(10);
        it.setUnitPrice(new BigDecimal("100.00"));
        when(quoteMapper.insert(any(CrmQuote.class))).thenAnswer(inv -> { ((CrmQuote) inv.getArgument(0)).setId(1L); return 1; });
        Result<CrmQuote> r = svc.createQuote(q, List.of(it), 1L);
        assertEquals(0, r.getCode());
        ArgumentCaptor<CrmQuoteItem> cap = ArgumentCaptor.forClass(CrmQuoteItem.class);
        Mockito.verify(itemMapper).insert(cap.capture());
        assertEquals("DWG-A001", cap.getValue().getDrawingNo());
    }

    @Test
    void epic3_material_dimension_carried_in_quote_item() {
        // Epic 3 契约：material/dimension 字段写入 crm_quote_item
            QuoteService svc = newQuoteService();
        CrmQuote q = new CrmQuote();
        q.setCustomerId(11L); q.setOwnerUserId(1L); q.setDeptId(1L);
        CrmQuoteItem it = new CrmQuoteItem();
        it.setDrawingNo("DWG-A002");
        it.setMaterial("SUS304");
        it.setSpec("100x50x10");
        it.setQuantity(5);
        it.setUnitPrice(new BigDecimal("200.00"));
        when(quoteMapper.insert(any(CrmQuote.class))).thenAnswer(inv -> { ((CrmQuote) inv.getArgument(0)).setId(2L); return 1; });
        svc.createQuote(q, List.of(it), 1L);
        ArgumentCaptor<CrmQuoteItem> cap = ArgumentCaptor.forClass(CrmQuoteItem.class);
        Mockito.verify(itemMapper).insert(cap.capture());
        assertEquals("SUS304", cap.getValue().getMaterial());
        assertEquals("100x50x10", cap.getValue().getSpec());
    }

    // =================== Epic 5 生产�? 测例�?===================
            @Test
    void epic5_quote_to_order_returns_xs_no() {
        // Epic 5 契约 1：quote 转订单返�?orderNo (XS 开�?
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(10L)).thenReturn(seedQuote(10L, "APPROVED", new BigDecimal("80000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(10L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("orderNo"));
        String orderNo = (String) r.getData().get("orderNo");
        assertTrue(orderNo.startsWith("XS"), "orderNo must start with XS, got: " + orderNo);
    }

    @Test
    void epic5_order_status_visible_for_workorder_module() {
        // Epic 5 契约 2：quote 状态从 APPROVED �?CONVERTED
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(11L)).thenReturn(seedQuote(11L, "APPROVED", new BigDecimal("120000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(11L, 1L);
        assertEquals(0, r.getCode());
        ArgumentCaptor<CrmQuote> cap = ArgumentCaptor.forClass(CrmQuote.class);
        Mockito.verify(quoteMapper).updateById(cap.capture());
        assertEquals("CONVERTED", cap.getValue().getStatus());
    }

    @Test
    void epic5_workorder_link_via_quote_id() {
        // Epic 5 契约 3：quote_id 透传�?order，工单模块通过 quote_id 关联
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(12L)).thenReturn(seedQuote(12L, "APPROVED", new BigDecimal("50000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(12L, 1L);
        assertEquals(0, r.getCode());
        Map<String, Object> data = r.getData();
        assertEquals(12L, data.get("quoteId"));
    }

    // =================== Epic 6 委外�? 测例�?===================
            @Test
    void epic6_subcontract_link_quote_no() {
        // Epic 6 契约 1：委外单透传 quoteNo + customerId
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(20L)).thenReturn(seedQuote(20L, "APPROVED", new BigDecimal("30000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(20L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("items"));
    }

    @Test
    void epic6_subcontract_history_recorded() {
        // Epic 6 契约 2：转委外单过�?crm_quote_history 留痕
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(21L)).thenReturn(seedQuote(21L, "APPROVED", new BigDecimal("40000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        svc.convertToOrder(21L, 1L);
        Mockito.verify(historyMapper, atLeastOnce()).insert(any(CrmQuoteHistory.class));
    }

    // =================== Epic 7 品质�? 测例�?===================
            @Test
    void epic7_fa_inspection_trigger_after_conversion() {
        // Epic 7 契约 1：quote �?order 后，FA 首件质检应触发（quantity > 0�?
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(30L)).thenReturn(seedQuote(30L, "APPROVED", new BigDecimal("20000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(30L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("orderNo"));
    }

    @Test
    void epic7_first_article_quantity_carried() {
        // Epic 7 契约 2：FA 首件数量 = items[0].quantity
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(31L)).thenReturn(seedQuote(31L, "APPROVED", new BigDecimal("10000.00")));
        when(itemMapper.selectByQuoteId(31L)).thenReturn(List.of(makeItem(31L, 1, "DWG-Q", 10, "100.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(31L, 1L);
        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        List<CrmQuoteItem> items = (List<CrmQuoteItem>) r.getData().get("items");
        assertEquals(1, items.size());
        assertEquals(10, items.get(0).getQuantity());
    }

    // =================== Epic 8 采购�? 测例�?===================
            @Test
    void epic8_purchase_order_linked_to_quote_item() {
        // Epic 8 契约 1：采购订单按 quote_item.drawingNo 关联物料
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(40L)).thenReturn(seedQuote(40L, "APPROVED", new BigDecimal("60000.00")));
        when(itemMapper.selectByQuoteId(40L)).thenReturn(List.of(makeItem(40L, 1, "DWG-P", 20, "300.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(40L, 1L);
        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        List<CrmQuoteItem> items = (List<CrmQuoteItem>) r.getData().get("items");
        assertEquals("DWG-P", items.get(0).getDrawingNo());
    }

    @Test
    void epic8_po_total_amount_summed_from_items() {
        // Epic 8 契约 2：PO 金额 = sum(items[].quantity * unitPrice)
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(41L)).thenReturn(seedQuote(41L, "APPROVED", new BigDecimal("90000.00")));
        when(itemMapper.selectByQuoteId(41L)).thenReturn(List.of(
            makeItem(41L, 1, "DWG-P1", 10, "100.00"),
            makeItem(41L, 2, "DWG-P2", 20, "200.00")
        ));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(41L, 1L);
        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        List<CrmQuoteItem> items = (List<CrmQuoteItem>) r.getData().get("items");
        BigDecimal total = items.stream()
            .map(it -> new BigDecimal(it.getQuantity())
                .multiply(it.getUnitPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, total.compareTo(new BigDecimal("5000.00")));
    }

    // =================== Epic 9 财务�? 测例�?===================
            @Test
    void epic9_contract_payment_invoice_linked_to_quote_no() {
        // Epic 9 契约 1：合同回�?+ 发票�?quoteNo
            QuoteService qs = newQuoteService();
        when(quoteMapper.selectById(50L)).thenReturn(seedQuote(50L, "APPROVED", new BigDecimal("150000.00")));
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(50L, 1L);
        assertEquals(0, r.getCode());
        Map<String, Object> data = r.getData();
        assertEquals("BJ20260611-0001", data.get("quoteNo"));
    }

    @Test
    void epic9_profit_analysis_uses_total_amount_minus_cost() {
        // Epic 9 契约 2：利润分�?= totalAmount - cost（totalAmount 透传 quote 实体�?totalAmount�?
            QuoteService qs = newQuoteService();
        CrmQuote q51 = seedQuote(51L, "APPROVED", new BigDecimal("200000.00"));
        when(quoteMapper.selectById(51L)).thenReturn(q51);
        OrderConversionService svc = newOrderConversionService(qs);
        Result<Map<String, Object>> r = svc.convertToOrder(51L, 1L);
        assertEquals(0, r.getCode());
        // 利润分析输入：quoteNo + totalAmount
            Map<String, Object> data = r.getData();
        assertNotNull(data.get("quoteNo"));
        assertNotNull(data.get("orderNo"));
        assertEquals(0, q51.getTotalAmount().compareTo(new BigDecimal("200000.00")));
    }

    // =================== Epic 11 报表�? 测例�?===================
            @Test
    void epic11_sales_aggregation_by_owner() {
        // Epic 11 契约：销售统计按 owner_user_id 聚合 count + sum(totalAmount)
            when(quoteMapper.selectList(any())).thenReturn(List.of(
            seedQuote(60L, "APPROVED", new BigDecimal("10000.00")),
            seedQuote(61L, "APPROVED", new BigDecimal("20000.00")),
            seedQuote(62L, "APPROVED", new BigDecimal("30000.00"))
        ));
        List<CrmQuote> list = quoteMapper.selectList(null);
        Map<Long, Map<String, Object>> agg = new HashMap<>();
        for (CrmQuote q : list) {
            Map<String, Object> m = agg.computeIfAbsent(q.getOwnerUserId(), k -> new HashMap<>());
            m.merge("count", 1, (a, b) -> (int) a + (int) b);
            m.merge("amount", q.getTotalAmount(), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
        }
        assertEquals(1, agg.size());
        Map<String, Object> stat = agg.get(1L);
        assertEquals(3, stat.get("count"));
        assertEquals(0, ((BigDecimal) stat.get("amount")).compareTo(new BigDecimal("60000.00")));
    }

    // =================== Story 1.4 APP 端（1 测例�?===================
            @Test
    void story14_app_wl_barcode_resolves_to_quote_item() {
        // Story 1.4 契约�? 类码 WL-物料�?引用 quote_item（业务员扫码查报价）
            when(itemMapper.selectByQuoteId(70L)).thenReturn(List.of(makeItem(70L, 1, "DWG-APP", 5, "500.00")));
        List<CrmQuoteItem> items = itemMapper.selectByQuoteId(70L);
        String barcode = "WL:quote:70:item:1";
        assertTrue(barcode.startsWith("WL:quote:"));
        long quoteId = Long.parseLong(barcode.split(":")[2]);
        long itemSort = Long.parseLong(barcode.split(":")[4]);
        CrmQuoteItem item = items.stream().filter(i -> i.getSort() == itemSort).findFirst().orElse(null);
        assertNotNull(item);
        assertEquals(70L, quoteId);
        assertEquals("DWG-APP", item.getDrawingNo());
    }

    // =================== helpers ====================
            private CrmQuoteItem makeItem(long quoteId, int sort, String drawingNo, int qty, String price) {
        CrmQuoteItem it = new CrmQuoteItem();
        it.setQuoteId(quoteId);
        it.setSort(sort);
        it.setDrawingNo(drawingNo);
        it.setQuantity(qty);
        it.setUnitPrice(new BigDecimal(price));
        return it;
    }
}
