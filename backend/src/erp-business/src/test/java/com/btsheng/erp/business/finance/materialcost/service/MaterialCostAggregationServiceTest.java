package com.btsheng.erp.business.finance.materialcost.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.materialcost.entity.CrmMaterialCostAggregation;
import com.btsheng.erp.business.finance.materialcost.mapper.CrmMaterialCostAggregationMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 · Story 1.40 · 财务·料号成本聚合 Service 单元测试（FR-9-5 V1.3.4 强化）
 * 18 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialCostAggregationServiceTest {

    @Mock private CrmMaterialCostAggregationMapper mapper;
    @Mock private DocNoGenerator docNoGenerator;

    private MaterialCostAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MaterialCostAggregationService(mapper, docNoGenerator);
        when(docNoGenerator.nextMaterialCostAggregationNo())
                .thenReturn("MC20260612-0001", "MC20260612-0002", "MC20260612-0003", "MC20260612-0004");
        when(mapper.insert(any(CrmMaterialCostAggregation.class))).thenAnswer(inv -> {
            CrmMaterialCostAggregation e = inv.getArgument(0);
            e.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(mapper.updateById(any(CrmMaterialCostAggregation.class))).thenReturn(1);
    }

    private CrmMaterialCostAggregation buildValid() {
        CrmMaterialCostAggregation r = new CrmMaterialCostAggregation();
        r.setMaterialId(2001L);
        r.setMaterialCode("M-AUTO-PART-001");
        r.setMaterialName("汽车配件 001");
        r.setAggMonth("2026-05");
        r.setVendorId(401L);
        r.setVendorName("上海金属材料");
        r.setQty(new BigDecimal("100"));
        r.setMaterialAmount(new BigDecimal("40000"));
        r.setProcessAmount(new BigDecimal("25000"));
        r.setOutsourceAmount(new BigDecimal("10000"));
        r.setManageAmount(new BigDecimal("6000"));
        r.setDepreciationAmount(new BigDecimal("4000"));
        return r;
    }

    // ====== aggregateByMaterial 7 测例 ======
            @Test
    @DisplayName("aggregate happy path · 5 段 + 总成本 + MC 前缀")
    void testAggregate_OK() {
        Result<Map<String, Object>> r = service.aggregateByMaterial(buildValid(), 703L);
        assertEquals(0, r.getCode());
        CrmMaterialCostAggregation e = (CrmMaterialCostAggregation) r.getData().get("aggregation");
        assertTrue(e.getAggNo().startsWith("MC"));
        // total = 40000+25000+10000+6000+4000 = 85000
            assertEquals(0, new BigDecimal("85000").compareTo(e.getTotalCost()));
        // unit = 85000/100 = 850
            assertEquals(0, new BigDecimal("850.0000").compareTo(e.getUnitCost()));
        // P1 修补 1：来源 5 段齐全
            assertEquals("1.9+1.10+1.15+1.26+1.14", e.getCostSources());
    }

    @Test
    @DisplayName("P1 修补 1：材料段为负 → 40001")
    void testAggregate_MaterialNegative() {
        CrmMaterialCostAggregation r = buildValid();
        r.setMaterialAmount(new BigDecimal("-1"));
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("P1 修补 1：加工段为负 → 40001")
    void testAggregate_ProcessNegative() {
        CrmMaterialCostAggregation r = buildValid();
        r.setProcessAmount(new BigDecimal("-1"));
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺物料编码 → 40001")
    void testAggregate_NoMaterial() {
        CrmMaterialCostAggregation r = buildValid();
        r.setMaterialCode(null);
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺月份 → 40001")
    void testAggregate_NoMonth() {
        CrmMaterialCostAggregation r = buildValid();
        r.setAggMonth(null);
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("缺数量 → 40001")
    void testAggregate_NoQty() {
        CrmMaterialCostAggregation r = buildValid();
        r.setQty(null);
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    @Test
    @DisplayName("数量为负 → 40001")
    void testAggregate_NegativeQty() {
        CrmMaterialCostAggregation r = buildValid();
        r.setQty(new BigDecimal("-1"));
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    // ====== getMaterialCost 4 测例 ======
            @Test
    @DisplayName("getMaterialCost · 按物料编码 · 5 段总金额 + by_month")
    void testGet_OK() {
        CrmMaterialCostAggregation e1 = buildValid();
        e1.setId(1L);
        e1.setAggMonth("2026-05");
        e1.setTotalCost(new BigDecimal("85000"));
        CrmMaterialCostAggregation e2 = buildValid();
        e2.setId(2L);
        e2.setAggMonth("2026-04");
        e2.setTotalCost(new BigDecimal("50000"));
        when(mapper.selectByMaterial("M-AUTO-PART-001")).thenReturn(List.of(e1, e2));
        Result<Map<String, Object>> r = service.getMaterialCost("M-AUTO-PART-001");
        assertEquals(0, r.getCode());
        // total = 85000 + 50000 = 135000
            assertEquals(0, new BigDecimal("135000").compareTo((BigDecimal) r.getData().get("total_cost")));
        @SuppressWarnings("unchecked")
        List<CrmMaterialCostAggregation> byMonth = (List<CrmMaterialCostAggregation>) r.getData().get("by_month");
        assertEquals(2, byMonth.size());
    }

    @Test
    @DisplayName("getMaterialCost · 找不到 → 40404")
    void testGet_NotFound() {
        when(mapper.selectByMaterial("M-UNKNOWN")).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> r = service.getMaterialCost("M-UNKNOWN");
        assertEquals(40404, r.getCode());
    }

    @Test
    @DisplayName("getMaterialCost · 缺编码 → 40001")
    void testGet_NoCode() {
        Result<Map<String, Object>> r = service.getMaterialCost(null);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("getMaterialCost · 5 段聚合 by_segment 完整")
    void testGet_BySegment() {
        CrmMaterialCostAggregation e = buildValid();
        e.setId(1L);
        e.setTotalCost(new BigDecimal("85000"));
        when(mapper.selectByMaterial(any())).thenReturn(List.of(e));
        Result<Map<String, Object>> r = service.getMaterialCost("M-AUTO-PART-001");
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> bySeg = (Map<String, BigDecimal>) r.getData().get("by_segment");
        assertEquals(0, new BigDecimal("40000").compareTo(bySeg.get("MATERIAL")));
        assertEquals(0, new BigDecimal("25000").compareTo(bySeg.get("PROCESS")));
        assertEquals(0, new BigDecimal("10000").compareTo(bySeg.get("OUTSOURCE")));
        assertEquals(0, new BigDecimal("6000").compareTo(bySeg.get("MANAGE")));
        assertEquals(0, new BigDecimal("4000").compareTo(bySeg.get("DEPRECIATION")));
    }

    // ====== getCostTrend 2 测例 ======
            @Test
    @DisplayName("P1 修补 3：趋势 12 月窗口")
    void testTrend_Window() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            Map<String, Object> e = new HashMap<>();
            e.put("material_code", "M-TEST");
            e.put("agg_month", String.format("2025-%02d", i));
            e.put("total_cost", new BigDecimal(1000L * i));
            rows.add(e);
        }
        when(mapper.selectCostTrend()).thenReturn(rows);
        Result<Map<String, Object>> r = service.getCostTrend();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) r.getData().get("trend");
        assertEquals(12, trend.size());
        assertEquals(12, r.getData().get("window_months"));
    }

    @Test
    @DisplayName("趋势 < 12 月直接返回")
    void testTrend_Small() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> e = new HashMap<>();
            e.put("material_code", "M-TEST");
            e.put("agg_month", String.format("2025-%02d", i));
            rows.add(e);
        }
        when(mapper.selectCostTrend()).thenReturn(rows);
        Result<Map<String, Object>> r = service.getCostTrend();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) r.getData().get("trend");
        assertEquals(5, trend.size());
    }

    // ====== compareVendors 3 测例 ======
            @Test
    @DisplayName("P1 修补 4：厂商对比 · 多 vendor 跨厂商")
    void testCompare_OK() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> v1 = new HashMap<>();
        v1.put("vendor_id", 401); v1.put("vendor_name", "上海金属材料");
        v1.put("total_cost", new BigDecimal("85000"));
        v1.put("avg_unit_cost", new BigDecimal("850.00"));
        v1.put("total_qty", new BigDecimal("100"));
        Map<String, Object> v2 = new HashMap<>();
        v2.put("vendor_id", 402); v2.put("vendor_name", "苏州精密件厂");
        v2.put("total_cost", new BigDecimal("91440"));
        v2.put("avg_unit_cost", new BigDecimal("457.20"));
        v2.put("total_qty", new BigDecimal("200"));
        rows.add(v1); rows.add(v2);
        when(mapper.selectVendorComparison("M-MACH-PART-002")).thenReturn(rows);
        Result<Map<String, Object>> r = service.compareVendors("M-MACH-PART-002");
        assertEquals(0, r.getCode());
        assertEquals(2, r.getData().get("vendor_count"));
    }

    @Test
    @DisplayName("compareVendors · 缺编码 → 40001")
    void testCompare_NoCode() {
        Result<Map<String, Object>> r = service.compareVendors(null);
        assertEquals(40001, r.getCode());
    }

    @Test
    @DisplayName("compareVendors · 0 厂商")
    void testCompare_Empty() {
        when(mapper.selectVendorComparison(any())).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> r = service.compareVendors("M-UNKNOWN");
        assertEquals(0, r.getCode());
        assertEquals(0, r.getData().get("vendor_count"));
    }

    // ====== exportMaterialCost 2 测例 ======
            @Test
    @DisplayName("export 默认 Excel · AC-9.5.2")
    void testExport_Excel() {
        when(mapper.selectByMaterial(any())).thenReturn(List.of(buildValid()));
        Result<Map<String, Object>> r = service.exportMaterialCost("M-AUTO-PART-001", null, 703L);
        assertEquals(0, r.getCode());
        assertEquals("excel", r.getData().get("format"));
        assertEquals(1, r.getData().get("row_count"));
    }

    @Test
    @DisplayName("export · format=pdf 合法")
    void testExport_Pdf() {
        when(mapper.selectByMaterial(any())).thenReturn(List.of(buildValid()));
        Result<Map<String, Object>> r = service.exportMaterialCost("M-AUTO-PART-001", "pdf", 703L);
        assertEquals(0, r.getCode());
        assertEquals("pdf", r.getData().get("format"));
    }

    @Test
    @DisplayName("export · format 非法 → 40002")
    void testExport_BadFormat() {
        Result<Map<String, Object>> r = service.exportMaterialCost("M-AUTO-PART-001", "xml", 703L);
        assertEquals(40002, r.getCode());
    }
}
