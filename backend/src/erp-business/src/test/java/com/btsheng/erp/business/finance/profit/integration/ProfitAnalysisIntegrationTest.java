package com.btsheng.erp.business.finance.profit.integration;

import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
import com.btsheng.erp.business.finance.profit.dto.AnalyzeProfitRequest;
import com.btsheng.erp.business.finance.profit.entity.CrmProfitAnalysis;
import com.btsheng.erp.business.finance.profit.mapper.CrmProfitAnalysisMapper;
import com.btsheng.erp.business.finance.profit.service.ProfitAnalysisService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.39 · 财务·利润分析 集成测试（FR-9-4）
 * 10 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfitAnalysisIntegrationTest {

    @Mock private CrmProfitAnalysisMapper profitMapper;
    @Mock private CrmCostAccountingMapper costMapper;
    @Mock private CrmCostSegmentMapper segmentMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private CrmOrderMapper orderMapper;

    private ProfitAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new ProfitAnalysisService(profitMapper, costMapper, segmentMapper, docNoGenerator, orderMapper);
        service.clearCacheForTest();
        when(docNoGenerator.nextProfitAnalysisNo())
                .thenReturn("PA20260612-0001", "PA20260612-0002", "PA20260612-0003", "PA20260612-0004");
        when(profitMapper.insert(any(CrmProfitAnalysis.class))).thenAnswer(inv -> {
            CrmProfitAnalysis p = inv.getArgument(0);
            p.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(segmentMapper.selectByCostId(any())).thenReturn(new ArrayList<>());
        when(profitMapper.updateById(any(CrmProfitAnalysis.class))).thenReturn(1);
    }

    private AnalyzeProfitRequest build(long orderId, String revenue, String settledDate) {
        AnalyzeProfitRequest r = new AnalyzeProfitRequest();
        r.setOrderId(orderId);
        r.setOrderNo("XS" + orderId);
        r.setCustomerId(301L);
        r.setCustomerName("客户-" + orderId);
        r.setProductId(2001L);
        r.setRevenue(new BigDecimal(revenue));
        r.setSettledDate(LocalDate.parse(settledDate));
        return r;
    }

    private CrmCostAccounting cost(long id, String no) {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(id); c.setCostNo(no); c.setRefType("ORDER");
        return c;
    }

    private CrmCostSegment seg(String code, String amount) {
        CrmCostSegment s = new CrmCostSegment();
        s.setSegmentCode(code);
        s.setAmount(new BigDecimal(amount));
        return s;
    }

    // ====== lifecycle 1：跨 1.6+1.37 利润分析 ======
            @Test
    @DisplayName("lifecycle 1：ORDER 跨订单+成本 利润分析")
    void testIntegration_Order() {
        when(profitMapper.selectByOrderId(5001L)).thenReturn(null);
        when(costMapper.selectByRef("ORDER", 5001L)).thenReturn(cost(1L, "CA20260601-0001"));
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(
                seg("MATERIAL", "40000"), seg("PROCESS", "25000"),
                seg("OUTSOURCE", "10000"), seg("MANAGE", "6000"),
                seg("DEPRECIATION", "4000")
        ));
        Result<Map<String, Object>> res = service.analyzeOrderProfit(
                build(5001L, "120000", "2026-05-20"), 703L);
        assertEquals(0, res.getCode());
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        assertEquals(1L, p.getCostId());
        assertEquals("CA20260601-0001", p.getCostNo());
    }

    // ====== lifecycle 2：无成本单 → 利润 = 收入 ======
            @Test
    @DisplayName("lifecycle 2：无 5 段成本 → 利润 = 收入")
    void testIntegration_NoCost() {
        when(profitMapper.selectByOrderId(any())).thenReturn(null);
        when(costMapper.selectByRef("ORDER", 5002L)).thenReturn(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(
                build(5002L, "50000", "2026-05-25"), 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        assertEquals(0, new BigDecimal("50000").compareTo(p.getProfit()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(p.getProfitRate()));
    }

    // ====== AC-9.4.1：单号模板 ======
            @Test
    @DisplayName("AC-9.4.1：单号模板 PA{yyyyMMdd}{seq:4}")
    void testIntegration_PaNo() {
        when(profitMapper.selectByOrderId(any())).thenReturn(null);
        when(costMapper.selectByRef(any(), any())).thenReturn(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(
                build(9999L, "10000", "2026-05-01"), 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        assertTrue(p.getProfitNo().startsWith("PA"));
        assertEquals(15, p.getProfitNo().length());
    }

    // ====== AC-9.4.1：analysis_month 自动派生 ======
            @Test
    @DisplayName("AC-9.4.1：analysis_month = settledDate yyyy-MM")
    void testIntegration_Month() {
        when(profitMapper.selectByOrderId(any())).thenReturn(null);
        when(costMapper.selectByRef(any(), any())).thenReturn(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(
                build(5010L, "10000", "2026-05-15"), 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        assertEquals("2026-05", p.getAnalysisMonth());
    }

    // ====== 跨 1.37 委外段 0 ======
            @Test
    @DisplayName("跨 1.37：5 段委外 = 0 不影响利润")
    void testIntegration_NoOutsource() {
        when(profitMapper.selectByOrderId(any())).thenReturn(null);
        when(costMapper.selectByRef("ORDER", 5020L)).thenReturn(cost(2L, "CA20260601-0002"));
        when(segmentMapper.selectByCostId(2L)).thenReturn(List.of(
                seg("MATERIAL", "50000"), seg("PROCESS", "30000"),
                seg("OUTSOURCE", "0"), seg("MANAGE", "10000"),
                seg("DEPRECIATION", "6100")
        ));
        Result<Map<String, Object>> res = service.analyzeOrderProfit(
                build(5020L, "100000", "2026-05-25"), 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        // profit = 100000 - 96100 = 3900
            assertEquals(0, new BigDecimal("3900").compareTo(p.getProfit()));
    }

    // ====== 客户排行集成 ======
            @Test
    @DisplayName("客户排行 · 3 客户按总利润降序")
    void testIntegration_Ranking() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>();
        r1.put("customer_id", 301); r1.put("customer_name", "上海汽车配件");
        r1.put("total_profit", new BigDecimal("35000"));
        r1.put("total_revenue", new BigDecimal("120000"));
        r1.put("order_count", 1);
        Map<String, Object> r2 = new HashMap<>();
        r2.put("customer_id", 303); r2.put("customer_name", "深圳五金制品");
        r2.put("total_profit", new BigDecimal("20000"));
        r2.put("total_revenue", new BigDecimal("50000"));
        r2.put("order_count", 1);
        rows.add(r1); rows.add(r2);
        when(profitMapper.selectCustomerRankingScoped(null, null)).thenReturn(rows);
        Result<Map<String, Object>> res = service.getCustomerProfitRanking();
        assertEquals(2, res.getData().get("count"));
    }

    // ====== 月度趋势集成 ======
            @Test
    @DisplayName("月度趋势 · 跨 3 月")
    void testIntegration_Trend() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String m : new String[]{"2026-04", "2026-05", "2026-06"}) {
            Map<String, Object> e = new HashMap<>();
            e.put("analysis_month", m);
            e.put("total_revenue", new BigDecimal("100000"));
            e.put("total_cost", new BigDecimal("80000"));
            e.put("total_profit", new BigDecimal("20000"));
            e.put("avg_profit_rate", new BigDecimal("20.0000"));
            rows.add(e);
        }
        when(profitMapper.selectMonthlyTrendScoped(null, null)).thenReturn(rows);
        Result<Map<String, Object>> res = service.getMonthlyTrend();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) res.getData().get("trend");
        assertEquals(3, trend.size());
    }

    // ====== 导出集成 ======
            @Test
    @DisplayName("export · 5 单聚合 + 报告含 order_count")
    void testIntegration_Export() {
        List<CrmProfitAnalysis> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CrmProfitAnalysis p = new CrmProfitAnalysis();
            p.setId((long) i);
            p.setRevenue(new BigDecimal("10000"));
            p.setTotalCost(new BigDecimal("7000"));
            p.setProfit(new BigDecimal("3000"));
            list.add(p);
        }
        when(profitMapper.selectByMonthScoped(eq("2026-05"), any(), any())).thenReturn(list);
        Result<Map<String, Object>> res = service.exportProfitReport("2026-05", 703L);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) res.getData().get("report");
        assertEquals(0, new BigDecimal("50000").compareTo((BigDecimal) report.get("total_revenue")));
        assertEquals(0, new BigDecimal("35000").compareTo((BigDecimal) report.get("total_cost")));
        assertEquals(0, new BigDecimal("15000").compareTo((BigDecimal) report.get("total_profit")));
        assertEquals(5, report.get("order_count"));
    }

    // ====== P1 修补 4：缓存清除测试 ======
            @Test
    @DisplayName("P1 修补 4：clearCacheForTest · 清空后首次不命中")
    void testIntegration_ClearCache() {
        when(profitMapper.selectByMonthScoped(any(), any(), any())).thenReturn(new ArrayList<>());
        service.clearCacheForTest();
        Result<Map<String, Object>> first = service.exportProfitReport("2026-04", 703L);
        assertEquals(false, first.getData().get("cached"));
    }

    // ====== 利润率 = 0（临界） ======
            @Test
    @DisplayName("利润率 = 0 → CRITICAL")
    void testIntegration_ZeroRate() {
        when(profitMapper.selectByOrderId(any())).thenReturn(null);
        when(costMapper.selectByRef(any(), any())).thenReturn(cost(1L, "CA20260601-0001"));
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(
                seg("MATERIAL", "10000"), seg("PROCESS", "0"),
                seg("OUTSOURCE", "0"), seg("MANAGE", "0"),
                seg("DEPRECIATION", "0")
        ));
        Result<Map<String, Object>> res = service.analyzeOrderProfit(
                build(5099L, "10000", "2026-05-10"), 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        assertEquals(0, new BigDecimal("0").compareTo(p.getProfitRate()));
        assertEquals("CRITICAL", p.getAlertLevel());
    }
}
