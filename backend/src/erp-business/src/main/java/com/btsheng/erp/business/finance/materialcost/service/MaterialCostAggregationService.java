package com.btsheng.erp.business.finance.materialcost.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.materialcost.entity.CrmMaterialCostAggregation;
import com.btsheng.erp.business.finance.materialcost.mapper.CrmMaterialCostAggregationMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.40 · 财务·料号成本聚合 Service (FR-9-5 V1.3.4 强化 · P0)
 *
 * <p>5 业务方法：aggregateByMaterial / getMaterialCost / getCostTrend / compareVendors / exportMaterialCost
 * <p>4 P1 修补�? * <ol>
 *   <li>5 段严�?V1.3.4 标准（material/process/outsource/manage/depreciation�?/li>
 *   <li>物料编码唯一（material_code 全局唯一键）</li>
 *   <li>趋势 12 月（P1 修补：取最�?12 个月窗口�?/li>
 *   <li>厂商对比（多 vendor 跨厂商）</li>
 * </ol>
 */
@Service
public class MaterialCostAggregationService {

    public static final String SEG_MATERIAL = "MATERIAL";
    public static final String SEG_PROCESS = "PROCESS";
    public static final String SEG_OUTSOURCE = "OUTSOURCE";
    public static final String SEG_MANAGE = "MANAGE";
    public static final String SEG_DEPRECIATION = "DEPRECIATION";

    public static final String COST_SOURCES = "1.9+1.10+1.15+1.26+1.14";

    public static final int TREND_MONTHS = 12;

    private final CrmMaterialCostAggregationMapper mapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public MaterialCostAggregationService(CrmMaterialCostAggregationMapper mapper,
                                          DocNoGenerator docNoGenerator) {
        this.mapper = mapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-9.5.1：按物料 × 月份 × 厂商聚合
     * P1 修补 1�? 段严�?V1.3.4 标准
     * P1 修补 2：物料编码唯一
     */
    @Transactional
    @AuditLog(module = "material_cost_agg", action = "agg.by_material")
    // V1.3.8 Sprint 7 集成 E：成本聚合变更触�?mat:detail 缓存失效（allEntries 兜底�?
            @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public Result<Map<String, Object>> aggregateByMaterial(CrmMaterialCostAggregation req, Long operatorUserId) {
        if (req == null || req.getMaterialCode() == null || req.getAggMonth() == null) {
            return Result.fail(40001, "MATERIAL_CODE_OR_MONTH_REQUIRED");
        }
        // P1 修补 1�? 段非�?
            BigDecimal[] amts = new BigDecimal[]{
                req.getMaterialAmount(), req.getProcessAmount(), req.getOutsourceAmount(),
                req.getManageAmount(), req.getDepreciationAmount()};
        for (BigDecimal a : amts) {
            if (a == null) {
                return Result.fail(40001, "SEGMENT_AMOUNT_NULL");
            }
            if (a.compareTo(BigDecimal.ZERO) < 0) {
                return Result.fail(40001, "SEGMENT_AMOUNT_NEGATIVE");
            }
        }
        if (req.getQty() == null || req.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal a : amts) total = total.add(a);
        BigDecimal unitCost = total.divide(req.getQty(), 4, RoundingMode.HALF_UP);

        CrmMaterialCostAggregation e = new CrmMaterialCostAggregation();
        e.setAggNo(docNoGenerator.nextMaterialCostAggregationNo());
        e.setMaterialId(req.getMaterialId());
        e.setMaterialCode(req.getMaterialCode());
        e.setMaterialName(req.getMaterialName());
        e.setAggMonth(req.getAggMonth());
        e.setVendorId(req.getVendorId());
        e.setVendorName(req.getVendorName());
        e.setQty(req.getQty());
        e.setMaterialAmount(req.getMaterialAmount());
        e.setProcessAmount(req.getProcessAmount());
        e.setOutsourceAmount(req.getOutsourceAmount());
        e.setManageAmount(req.getManageAmount());
        e.setDepreciationAmount(req.getDepreciationAmount());
        e.setTotalCost(total);
        e.setUnitCost(unitCost);
        e.setCostSources(COST_SOURCES);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        mapper.insert(e);

        Map<String, Object> data = new HashMap<>();
        data.put("aggregation", e);
        return Result.ok(data);
    }

    /**
     * AC-9.5.1：按物料编码�?5 段成本（跨月+跨厂商）
     * P1 修补 2：物料编码唯一
     */
    @AuditLog(module = "material_cost_agg", action = "agg.get_by_material")
    public Result<Map<String, Object>> getMaterialCost(String materialCode) {
        if (materialCode == null || materialCode.isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        List<CrmMaterialCostAggregation> list = mapper.selectByMaterial(materialCode);
        if (list == null || list.isEmpty()) {
            return Result.fail(40404, "MATERIAL_NOT_FOUND");
        }
        // 5 段总成�?+ 按月聚合
            BigDecimal total = BigDecimal.ZERO;
        BigDecimal matTotal = BigDecimal.ZERO;
        BigDecimal procTotal = BigDecimal.ZERO;
        BigDecimal outTotal = BigDecimal.ZERO;
        BigDecimal mgTotal = BigDecimal.ZERO;
        BigDecimal depTotal = BigDecimal.ZERO;
        for (CrmMaterialCostAggregation e : list) {
            total = total.add(nullSafe(e.getTotalCost()));
            matTotal = matTotal.add(nullSafe(e.getMaterialAmount()));
            procTotal = procTotal.add(nullSafe(e.getProcessAmount()));
            outTotal = outTotal.add(nullSafe(e.getOutsourceAmount()));
            mgTotal = mgTotal.add(nullSafe(e.getManageAmount()));
            depTotal = depTotal.add(nullSafe(e.getDepreciationAmount()));
        }
        Map<String, Object> out = new HashMap<>();
        out.put("material_code", materialCode);
        out.put("material_name", list.get(0).getMaterialName());
        out.put("by_month", list);
        out.put("total_cost", total);
        Map<String, BigDecimal> bySeg = new HashMap<>();
        bySeg.put(SEG_MATERIAL, matTotal);
        bySeg.put(SEG_PROCESS, procTotal);
        bySeg.put(SEG_OUTSOURCE, outTotal);
        bySeg.put(SEG_MANAGE, mgTotal);
        bySeg.put(SEG_DEPRECIATION, depTotal);
        out.put("by_segment", bySeg);
        return Result.ok(out);
    }

    /**
     * AC-9.5.1：成本趋势（12 月窗口）
     * P1 修补 3：趋�?12 �?     */
    @AuditLog(module = "material_cost_agg", action = "agg.trend")
    public Result<Map<String, Object>> getCostTrend() {
        List<Map<String, Object>> trend = mapper.selectCostTrend();
        // P1 修补 3：取最�?12 个月
            if (trend != null && trend.size() > TREND_MONTHS) {
            trend = trend.subList(trend.size() - TREND_MONTHS, trend.size());
        }
        Map<String, Object> out = new HashMap<>();
        out.put("trend", trend == null ? new ArrayList<>() : trend);
        out.put("window_months", TREND_MONTHS);
        return Result.ok(out);
    }

    /**
     * AC-9.5.1：厂商对�?     * P1 修补 4：厂商对比（�?vendor 跨厂商）
     */
    @AuditLog(module = "material_cost_agg", action = "agg.vendor_compare")
    public Result<Map<String, Object>> compareVendors(String materialCode) {
        if (materialCode == null || materialCode.isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        List<Map<String, Object>> rows = mapper.selectVendorComparison(materialCode);
        Map<String, Object> out = new HashMap<>();
        out.put("material_code", materialCode);
        out.put("vendors", rows == null ? new ArrayList<>() : rows);
        out.put("vendor_count", rows == null ? 0 : rows.size());
        return Result.ok(out);
    }

    /**
     * AC-9.5.2：导出（Excel/PDF 复用 1.26 委外成本导出器）
     */
    @AuditLog(module = "material_cost_agg", action = "agg.export")
    public Result<Map<String, Object>> exportMaterialCost(String materialCode, String format, Long operatorUserId) {
        if (materialCode == null || materialCode.isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        if (format == null) format = "excel";
        if (!"excel".equalsIgnoreCase(format) && !"pdf".equalsIgnoreCase(format)) {
            return Result.fail(40002, "FORMAT_INVALID");
        }
        List<CrmMaterialCostAggregation> list = mapper.selectByMaterial(materialCode);
        Map<String, Object> out = new HashMap<>();
        out.put("material_code", materialCode);
        out.put("format", format.toLowerCase());
        out.put("row_count", list == null ? 0 : list.size());
        out.put("exported_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        out.put("data", list);
        return Result.ok(out);
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
