package com.btsheng.erp.business.finance.profit.service;

import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
import com.btsheng.erp.business.finance.profit.dto.AnalyzeProfitRequest;
import com.btsheng.erp.business.finance.profit.entity.CrmProfitAnalysis;
import com.btsheng.erp.business.finance.profit.mapper.CrmProfitAnalysisMapper;
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
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.39 · 财务·利润分析 Service 单元测试（FR-9-4）
 * 14 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfitAnalysisServiceTest {

    @Mock private CrmProfitAnalysisMapper profitMapper;
    @Mock private CrmCostAccountingMapper costMapper;
    @Mock private CrmCostSegmentMapper segmentMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private CrmOrderMapper orderMapper;
    @Mock private ProfitThresholdResolver thresholdResolver;

    private ProfitAnalysisService service;

    @BeforeEach
    void setUp() {
        when(thresholdResolver.resolveAlertLevel(any())).thenAnswer(inv -> {
            BigDecimal rate = inv.getArgument(0);
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                return ProfitAnalysisService.ALERT_CRITICAL;
            }
            if (rate.compareTo(new BigDecimal("5")) < 0) {
                return ProfitAnalysisService.ALERT_CRITICAL;
            }
            if (rate.compareTo(new BigDecimal("10")) < 0) {
                return ProfitAnalysisService.ALERT_WARNING;
            }
            return ProfitAnalysisService.ALERT_NORMAL;
        });
        service = new ProfitAnalysisService(profitMapper, costMapper, segmentMapper, docNoGenerator, orderMapper, thresholdResolver);
        when(docNoGenerator.nextProfitAnalysisNo())
                .thenReturn("PA20260612-0001", "PA20260612-0002", "PA20260612-0003");
        when(profitMapper.insert(any(CrmProfitAnalysis.class))).thenAnswer(inv -> {
            CrmProfitAnalysis p = inv.getArgument(0);
            p.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(segmentMapper.selectByCostId(any())).thenReturn(new ArrayList<>());
        when(profitMapper.updateById(any(CrmProfitAnalysis.class))).thenReturn(1);
    }

    private AnalyzeProfitRequest buildValid() {
        AnalyzeProfitRequest r = new AnalyzeProfitRequest();
        r.setOrderId(5001L);
        r.setOrderNo("XS20260501-0001");
        r.setCustomerId(301L);
        r.setCustomerName("上海汽车配件");
        r.setProductId(2001L);
        r.setProductCode("M-AUTO-PART-001");
        r.setProductName("汽车配件 001");
        r.setRevenue(new BigDecimal("120000"));
        r.setSettledDate(LocalDate.now().minusDays(5));
        return r;
    }

    private CrmCostAccounting mockCost(long id, String costNo) {
        CrmCostAccounting c = new CrmCostAccounting();
        c.setId(id);
        c.setCostNo(costNo);
        c.setRefType("ORDER");
        return c;
    }

    private CrmCostSegment seg(String code, String amount) {
        CrmCostSegment s = new CrmCostSegment();
        s.setSegmentCode(code);
        s.setAmount(new BigDecimal(amount));
        return s;
    }

    // ====== analyzeOrderProfit 8 测例 ======
            @Test
    @DisplayName("analyze happy path · 利润 = 收入 - 5 段成本 + PA 前缀")
    void testAnalyze_OK() {
        when(profitMapper.selectByOrderId(5001L)).thenReturn(null);
        when(costMapper.selectByRef("ORDER", 5001L)).thenReturn(mockCost(1L, "CA20260601-0001"));
        when(segmentMapper.selectByCostId(1L)).thenReturn(List.of(
                seg("MATERIAL", "40000"), seg("PROCESS", "25000"),
                seg("OUTSOURCE", "10000"), seg("MANAGE", "6000"),
                seg("DEPRECIATION", "4000")
        ));
        Result<Map<String, Object>> r = service.analyzeOrderProfit(buildValid(), 703L);
        assertEquals(0, r.getCode());
        CrmProfitAnalysis p = (CrmProfitAnalysis) r.getData().get("profit");
        assertTrue(p.getProfitNo().startsWith("PA"));
        // profit = 120000 - 85000 = 35000
            assertEquals(0, new BigDecimal("35000").compareTo(p.getProfit()));
        // rate = 35000/120000*100 = 29.1667
            assertEquals(0, new BigDecimal("29.1667").compareTo(p.getProfitRate()));
    }

    @Test
    @DisplayName("P1 修补 2：利润为负 → 利润率 -125%（亏损订单）")
    void testAnalyze_Loss() {
        when(profitMapper.selectByOrderId(5001L)).thenReturn(null);
        when(costMapper.selectByRef("ORDER", 5001L)).thenReturn(mockCost(4L, "CA20260601-0004"));
        when(segmentMapper.selectByCostId(4L)).thenReturn(List.of(
                seg("MATERIAL", "20000"), seg("PROCESS", "15000"),
                seg("OUTSOURCE", "5000"), seg("MANAGE", "3000"),
                seg("DEPRECIATION", "2000")
        ));
        AnalyzeProfitRequest r = buildValid();
        r.setRevenue(new BigDecimal("20000"));
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        // profit = 20000 - 45000 = -25000
            assertEquals(0, new BigDecimal("-25000").compareTo(p.getProfit()));
        // rate = -25000/20000*100 = -125
            assertEquals(0, new BigDecimal("-125.0000").compareTo(p.getProfitRate()));
        assertEquals("CRITICAL", p.getAlertLevel());
    }

    @Test
    @DisplayName("P1 修补 2：利润率 < 5% → WARNING")
    void testAnalyze_Warning() {
        when(profitMapper.selectByOrderId(5001L)).thenReturn(null);
        when(costMapper.selectByRef("ORDER", 5001L)).thenReturn(mockCost(2L, "CA20260601-0002"));
        when(segmentMapper.selectByCostId(2L)).thenReturn(List.of(
                seg("MATERIAL", "50000"), seg("PROCESS", "30000"),
                seg("OUTSOURCE", "0"), seg("MANAGE", "10000"),
                seg("DEPRECIATION", "6100")
        ));
        AnalyzeProfitRequest r = buildValid();
        r.setRevenue(new BigDecimal("100000"));
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        CrmProfitAnalysis p = (CrmProfitAnalysis) res.getData().get("profit");
        // profit = 100000 - 96100 = 3900
            assertEquals(0, new BigDecimal("3900").compareTo(p.getProfit()));
        // rate = 3.9%
            assertEquals(0, new BigDecimal("3.9000").compareTo(p.getProfitRate()));
        assertEquals("WARNING", p.getAlertLevel());
    }

    @Test
    @DisplayName("缺订单 ID → 40001")
    void testAnalyze_NoOrder() {
        AnalyzeProfitRequest r = buildValid();
        r.setOrderId(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺收入 → 40001")
    void testAnalyze_NoRevenue() {
        AnalyzeProfitRequest r = buildValid();
        r.setRevenue(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("收入为负 → 40001")
    void testAnalyze_NegativeRevenue() {
        AnalyzeProfitRequest r = buildValid();
        r.setRevenue(new BigDecimal("-1"));
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺客户名 → 40001")
    void testAnalyze_NoCustomer() {
        AnalyzeProfitRequest r = buildValid();
        r.setCustomerName(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺 SETTLED 日期 → 40001")
    void testAnalyze_NoSettledDate() {
        AnalyzeProfitRequest r = buildValid();
        r.setSettledDate(null);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("订单重复 → 40902")
    void testAnalyze_Duplicate() {
        CrmProfitAnalysis existed = new CrmProfitAnalysis();
        existed.setId(99L);
        when(profitMapper.selectByOrderId(5001L)).thenReturn(existed);
        Result<Map<String, Object>> res = service.analyzeOrderProfit(buildValid(), 703L);
        assertEquals(40902, res.getCode());
    }

    // ====== getCustomerProfitRanking 2 测例 ======
            @Test
    @DisplayName("getCustomerProfitRanking · 客户排行")
    void testRanking() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>();
        r1.put("customer_id", 301); r1.put("customer_name", "上海汽车配件");
        r1.put("total_profit", new BigDecimal("35000"));
        r1.put("total_revenue", new BigDecimal("120000"));
        r1.put("order_count", 1);
        rows.add(r1);
        when(profitMapper.selectCustomerRankingScoped(null, null)).thenReturn(rows);
        Result<Map<String, Object>> res = service.getCustomerProfitRanking();
        assertEquals(0, res.getCode());
        assertEquals(1, res.getData().get("count"));
    }

    @Test
    @DisplayName("getCustomerProfitRanking · 空数据")
    void testRanking_Empty() {
        when(profitMapper.selectCustomerRankingScoped(null, null)).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> res = service.getCustomerProfitRanking();
        assertEquals(0, res.getCode());
        assertEquals(0, res.getData().get("count"));
    }

    // ====== getMonthlyTrend 2 测例 ======
            @Test
    @DisplayName("getMonthlyTrend · 月度趋势")
    void testTrend() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("analysis_month", "2026-05");
        m.put("total_revenue", new BigDecimal("120000"));
        m.put("total_cost", new BigDecimal("85000"));
        m.put("total_profit", new BigDecimal("35000"));
        m.put("avg_profit_rate", new BigDecimal("29.1667"));
        rows.add(m);
        when(profitMapper.selectMonthlyTrendScoped(null, null)).thenReturn(rows);
        Result<Map<String, Object>> res = service.getMonthlyTrend();
        assertEquals(0, res.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) res.getData().get("trend");
        assertEquals(1, trend.size());
    }

    @Test
    @DisplayName("getMonthlyTrend · 空数据")
    void testTrend_Empty() {
        when(profitMapper.selectMonthlyTrendScoped(null, null)).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> res = service.getMonthlyTrend();
        assertEquals(0, res.getCode());
    }

    // ====== exportProfitReport 2 测例 ======
            @Test
    @DisplayName("exportProfitReport · 不带 month 默认本月 + 不缓存首次")
    void testExport_FirstTime() {
        when(profitMapper.selectByMonthScoped(any(), any(), any())).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> res = service.exportProfitReport(null, 703L);
        assertEquals(0, res.getCode());
        assertEquals(false, res.getData().get("cached"));
    }

    @Test
    @DisplayName("P1 修补 4：PDF 1h 缓存 · 第二次命中")
    void testExport_Cache() {
        when(profitMapper.selectByMonthScoped(any(), any(), any())).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> first = service.exportProfitReport("2026-06", 703L);
        assertEquals(false, first.getData().get("cached"));
        Result<Map<String, Object>> second = service.exportProfitReport("2026-06", 703L);
        assertEquals(true, second.getData().get("cached"));
        assertNotNull(second.getData().get("cache_age_ms"));
    }
}
