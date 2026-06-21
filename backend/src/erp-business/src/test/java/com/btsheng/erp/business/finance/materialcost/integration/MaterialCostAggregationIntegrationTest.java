package com.btsheng.erp.business.finance.materialcost.integration;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.materialcost.entity.CrmMaterialCostAggregation;
import com.btsheng.erp.business.finance.materialcost.mapper.CrmMaterialCostAggregationMapper;
import com.btsheng.erp.business.finance.materialcost.service.MaterialCostAggregationService;
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
 * V1.3.7 · Story 1.40 · 财务·料号成本聚合 集成测试（FR-9-5 V1.3.4 强化）
 * 12 测例
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialCostAggregationIntegrationTest {

    @Mock private CrmMaterialCostAggregationMapper mapper;
    @Mock private DocNoGenerator docNoGenerator;

    private MaterialCostAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MaterialCostAggregationService(mapper, docNoGenerator);
        when(docNoGenerator.nextMaterialCostAggregationNo())
                .thenReturn("MC20260612-0001", "MC20260612-0002", "MC20260612-0003", "MC20260612-0004", "MC20260612-0005");
        when(mapper.insert(any(CrmMaterialCostAggregation.class))).thenAnswer(inv -> {
            CrmMaterialCostAggregation e = inv.getArgument(0);
            e.setId(System.nanoTime() & 0x7fffffff);
            return 1;
        });
        when(mapper.updateById(any(CrmMaterialCostAggregation.class))).thenReturn(1);
    }

    private CrmMaterialCostAggregation build(String materialCode, String materialName,
                                             String month, long vendorId, String vendorName,
                                             String mat, String proc, String out, String mg, String dep,
                                             String qty) {
        CrmMaterialCostAggregation r = new CrmMaterialCostAggregation();
        r.setMaterialId(2001L);
        r.setMaterialCode(materialCode);
        r.setMaterialName(materialName);
        r.setAggMonth(month);
        r.setVendorId(vendorId);
        r.setVendorName(vendorName);
        r.setQty(new BigDecimal(qty));
        r.setMaterialAmount(new BigDecimal(mat));
        r.setProcessAmount(new BigDecimal(proc));
        r.setOutsourceAmount(new BigDecimal(out));
        r.setManageAmount(new BigDecimal(mg));
        r.setDepreciationAmount(new BigDecimal(dep));
        return r;
    }

    private CrmMaterialCostAggregation row(long id, String materialCode, String materialName,
                                           String month, long vendorId, String vendorName,
                                           String mat, String proc, String out, String mg, String dep,
                                           String total) {
        CrmMaterialCostAggregation e = new CrmMaterialCostAggregation();
        e.setId(id);
        e.setMaterialCode(materialCode);
        e.setMaterialName(materialName);
        e.setAggMonth(month);
        e.setVendorId(vendorId);
        e.setVendorName(vendorName);
        e.setMaterialAmount(new BigDecimal(mat));
        e.setProcessAmount(new BigDecimal(proc));
        e.setOutsourceAmount(new BigDecimal(out));
        e.setManageAmount(new BigDecimal(mg));
        e.setDepreciationAmount(new BigDecimal(dep));
        e.setTotalCost(new BigDecimal(total));
        e.setQty(new BigDecimal("100"));
        e.setUnitCost(new BigDecimal(total).divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        return e;
    }

    // ====== lifecycle 1：5 段聚合跨月跨厂商 ======
            @Test
    @DisplayName("lifecycle 1：物料 2001 跨 3 月聚合 → 3 行")
    void testIntegration_MultiMonth() {
        when(mapper.selectByMaterial("M-AUTO-PART-001")).thenReturn(List.of(
                row(1L, "M-AUTO-PART-001", "汽车配件 001", "2026-04", 401, "上海金属材料",
                        "40000", "25000", "10000", "6000", "4000", "85000"),
                row(2L, "M-AUTO-PART-001", "汽车配件 001", "2026-05", 401, "上海金属材料",
                        "60000", "36000", "15000", "9000", "6000", "126000"),
                row(3L, "M-AUTO-PART-001", "汽车配件 001", "2026-06", 401, "上海金属材料",
                        "32000", "20000", "8000", "4800", "3200", "68000")
        ));
        Result<Map<String, Object>> res = service.getMaterialCost("M-AUTO-PART-001");
        assertEquals(0, res.getCode());
        @SuppressWarnings("unchecked")
        List<CrmMaterialCostAggregation> byMonth = (List<CrmMaterialCostAggregation>) res.getData().get("by_month");
        assertEquals(3, byMonth.size());
        // total = 85000+126000+68000 = 279000
            assertEquals(0, new BigDecimal("279000").compareTo((BigDecimal) res.getData().get("total_cost")));
    }

    // ====== AC-9.5.1：单号模板 ======
            @Test
    @DisplayName("AC-9.5.1：单号模板 MC{yyyyMMdd}{seq:4}")
    void testIntegration_McNo() {
        Result<Map<String, Object>> res = service.aggregateByMaterial(
                build("M-AUTO-PART-001", "汽车配件 001", "2026-05", 401, "上海金属材料",
                        "1000", "1000", "0", "500", "500", "100"), 703L);
        CrmMaterialCostAggregation e = (CrmMaterialCostAggregation) res.getData().get("aggregation");
        assertTrue(e.getAggNo().startsWith("MC"));
        assertEquals(15, e.getAggNo().length());
    }

    // ====== AC-9.5.1：5 段来源 V1.3.4 标准 ======
            @Test
    @DisplayName("AC-9.5.1：5 段来源 cost_sources 必填 V1.3.4")
    void testIntegration_Sources() {
        Result<Map<String, Object>> res = service.aggregateByMaterial(
                build("M-MACH-PART-002", "机械配件 002", "2026-05", 401, "上海金属材料",
                        "5000", "3000", "0", "1000", "500", "100"), 703L);
        CrmMaterialCostAggregation e = (CrmMaterialCostAggregation) res.getData().get("aggregation");
        assertEquals("1.9+1.10+1.15+1.26+1.14", e.getCostSources());
    }

    // ====== lifecycle 2：物料编码唯一 ======
            @Test
    @DisplayName("P1 修补 2：物料编码唯一（按编码查得 1 物料多行）")
    void testIntegration_UniqueMaterial() {
        when(mapper.selectByMaterial("M-HARD-PART-003")).thenReturn(List.of(
                row(7L, "M-HARD-PART-003", "五金配件 003", "2026-04", 403, "深圳五金厂",
                        "18000", "8000", "0", "3000", "1000", "30000"),
                row(8L, "M-HARD-PART-003", "五金配件 003", "2026-05", 403, "深圳五金厂",
                        "15000", "6666.67", "0", "2500", "833.33", "25000"),
                row(9L, "M-HARD-PART-003", "五金配件 003", "2026-06", 403, "深圳五金厂",
                        "21000", "9333.33", "0", "3500", "1166.67", "35000")
        ));
        Result<Map<String, Object>> res = service.getMaterialCost("M-HARD-PART-003");
        assertEquals("M-HARD-PART-003", res.getData().get("material_code"));
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> bySeg = (Map<String, BigDecimal>) res.getData().get("by_segment");
        // material = 18000+15000+21000 = 54000
            assertEquals(0, new BigDecimal("54000").compareTo(bySeg.get("MATERIAL")));
    }

    // ====== AC-9.5.1：跨厂商对比 ======
            @Test
    @DisplayName("AC-9.5.1：物料 2002 跨 2 厂商对比")
    void testIntegration_CrossVendor() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> v1 = new HashMap<>();
        v1.put("vendor_id", 401); v1.put("vendor_name", "上海金属材料");
        v1.put("total_cost", new BigDecimal("96100"));
        v1.put("avg_unit_cost", new BigDecimal("480.50"));
        v1.put("total_qty", new BigDecimal("200"));
        Map<String, Object> v2 = new HashMap<>();
        v2.put("vendor_id", 402); v2.put("vendor_name", "苏州精密件厂");
        v2.put("total_cost", new BigDecimal("91440"));
        v2.put("avg_unit_cost", new BigDecimal("457.20"));
        v2.put("total_qty", new BigDecimal("200"));
        rows.add(v1); rows.add(v2);
        when(mapper.selectVendorComparison("M-MACH-PART-002")).thenReturn(rows);
        Result<Map<String, Object>> res = service.compareVendors("M-MACH-PART-002");
        assertEquals(2, res.getData().get("vendor_count"));
    }

    // ====== AC-9.5.1：单厂商物料 ======
            @Test
    @DisplayName("AC-9.5.1：单厂商物料对比 · 1 厂商")
    void testIntegration_SingleVendor() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> v1 = new HashMap<>();
        v1.put("vendor_id", 404); v1.put("vendor_name", "杭州电气配件");
        v1.put("total_cost", new BigDecimal("45000"));
        v1.put("avg_unit_cost", new BigDecimal("900.00"));
        v1.put("total_qty", new BigDecimal("50"));
        rows.add(v1);
        when(mapper.selectVendorComparison("M-MOTOR-004")).thenReturn(rows);
        Result<Map<String, Object>> res = service.compareVendors("M-MOTOR-004");
        assertEquals(1, res.getData().get("vendor_count"));
    }

    // ====== 跨 BOM 1.9 + 工艺 1.10 + 工单 1.15 ======
            @Test
    @DisplayName("跨模块 1.9+1.10+1.15：物料 2001 5 段全非 0")
    void testIntegration_CrossModule() {
        Result<Map<String, Object>> res = service.aggregateByMaterial(
                build("M-AUTO-PART-001", "汽车配件 001", "2026-05", 401, "上海金属材料",
                        "40000", "25000", "10000", "6000", "4000", "100"), 703L);
        CrmMaterialCostAggregation e = (CrmMaterialCostAggregation) res.getData().get("aggregation");
        // 5 段全非 0 → 总成本 = 85000
            assertEquals(0, new BigDecimal("85000").compareTo(e.getTotalCost()));
        assertTrue(e.getMaterialAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(e.getProcessAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    // ====== AC-9.5.2：导出 PDF ======
            @Test
    @DisplayName("AC-9.5.2：导出 PDF 格式")
    void testIntegration_ExportPdf() {
        when(mapper.selectByMaterial("M-AUTO-PART-001")).thenReturn(List.of(
                row(1L, "M-AUTO-PART-001", "汽车配件 001", "2026-05", 401, "上海金属材料",
                        "40000", "25000", "10000", "6000", "4000", "85000")
        ));
        Result<Map<String, Object>> res = service.exportMaterialCost("M-AUTO-PART-001", "pdf", 703L);
        assertEquals(0, res.getCode());
        assertEquals("pdf", res.getData().get("format"));
        assertEquals(1, res.getData().get("row_count"));
        assertNotNull(res.getData().get("exported_at"));
    }

    // ====== AC-9.5.2：导出 Excel ======
            @Test
    @DisplayName("AC-9.5.2：导出 Excel 格式（默认）")
    void testIntegration_ExportExcel() {
        when(mapper.selectByMaterial("M-AUTO-PART-001")).thenReturn(List.of(
                row(1L, "M-AUTO-PART-001", "汽车配件 001", "2026-05", 401, "上海金属材料",
                        "40000", "25000", "10000", "6000", "4000", "85000")
        ));
        Result<Map<String, Object>> res = service.exportMaterialCost("M-AUTO-PART-001", null, 703L);
        assertEquals("excel", res.getData().get("format"));
    }

    // ====== P1 修补 1：5 段严格 V1.3.4 ======
            @Test
    @DisplayName("P1 修补 1：5 段严格 V1.3.4 · 委外段为负拒绝")
    void testIntegration_StrictV134() {
        CrmMaterialCostAggregation r = build("M-AUTO-PART-001", "汽车配件 001", "2026-05", 401, "上海金属材料",
                "40000", "25000", "10000", "6000", "4000", "100");
        r.setOutsourceAmount(new BigDecimal("-100"));
        Result<Map<String, Object>> res = service.aggregateByMaterial(r, 703L);
        assertEquals(40001, res.getCode());
    }

    // ====== P1 修补 3：趋势 12 月窗口 ======
            @Test
    @DisplayName("P1 修补 3：趋势 18 月 → 截断到 12 月")
    void testIntegration_TrendCut() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            Map<String, Object> e = new HashMap<>();
            e.put("material_code", "M-AUTO-PART-001");
            e.put("agg_month", String.format("2025-%02d", i));
            e.put("total_cost", new BigDecimal(1000L * i));
            rows.add(e);
        }
        when(mapper.selectCostTrend()).thenReturn(rows);
        Result<Map<String, Object>> res = service.getCostTrend();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) res.getData().get("trend");
        assertEquals(12, trend.size());
    }

    // ====== 5 段全 0 合法 ======
            @Test
    @DisplayName("5 段全 0 · total=0 合法")
    void testIntegration_AllZero() {
        Result<Map<String, Object>> res = service.aggregateByMaterial(
                build("M-NEW", "新物料", "2026-05", 401, "新厂商",
                        "0", "0", "0", "0", "0", "1"), 703L);
        CrmMaterialCostAggregation e = (CrmMaterialCostAggregation) res.getData().get("aggregation");
        assertEquals(0, new BigDecimal("0").compareTo(e.getTotalCost()));
    }

    // ====== 跨厂商成本差异 ======
            @Test
    @DisplayName("跨厂商：苏州厂商单价比上海厂商低 4.85%")
    void testIntegration_VendorDiff() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> v1 = new HashMap<>();
        v1.put("vendor_id", 401); v1.put("vendor_name", "上海金属材料");
        v1.put("total_cost", new BigDecimal("96100"));
        v1.put("avg_unit_cost", new BigDecimal("480.50"));
        Map<String, Object> v2 = new HashMap<>();
        v2.put("vendor_id", 402); v2.put("vendor_name", "苏州精密件厂");
        v2.put("total_cost", new BigDecimal("91440"));
        v2.put("avg_unit_cost", new BigDecimal("457.20"));
        rows.add(v1); rows.add(v2);
        when(mapper.selectVendorComparison("M-MACH-PART-002")).thenReturn(rows);
        Result<Map<String, Object>> res = service.compareVendors("M-MACH-PART-002");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vendors = (List<Map<String, Object>>) res.getData().get("vendors");
        // 苏州更低
            assertEquals(0, new BigDecimal("480.50").compareTo((BigDecimal) vendors.get(0).get("avg_unit_cost")));
        assertEquals(0, new BigDecimal("457.20").compareTo((BigDecimal) vendors.get(1).get("avg_unit_cost")));
        // 苏州便宜 23.30 元/件
            assertEquals(0, new BigDecimal("23.30").compareTo(
                ((BigDecimal) vendors.get(0).get("avg_unit_cost")).subtract((BigDecimal) vendors.get(1).get("avg_unit_cost"))));
    }
}
