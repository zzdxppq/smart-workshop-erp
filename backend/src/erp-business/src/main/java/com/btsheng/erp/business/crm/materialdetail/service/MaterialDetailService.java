package com.btsheng.erp.business.crm.materialdetail.service;

import com.btsheng.erp.business.integration.client.ProductionProductRouteClient;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialCategory;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialCategoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.materialdetail.dto.ChangeLogEntry;
import com.btsheng.erp.business.crm.materialdetail.dto.MaterialDetailDTO;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 · Story 2.1 · 料号详情页聚合 Service（7 Tab）
 *
 * <p>工艺路线 Tab / {@link #getProcessRoute} 已对接 erp-production
 * {@code GET /products/{id}/routes}（E3-S3.4）。
 */
@Service
public class MaterialDetailService {

    private static final Logger log = LoggerFactory.getLogger(MaterialDetailService.class);

    private final ProductionProductRouteClient productRouteClient;
    private final CrmMaterialMapper materialMapper;
    private final CrmDrawingMapper drawingMapper;
    private final CrmMaterialCategoryMapper categoryMapper;

    @Autowired
    public MaterialDetailService(ProductionProductRouteClient productRouteClient,
                                 CrmMaterialMapper materialMapper,
                                 CrmDrawingMapper drawingMapper,
                                 CrmMaterialCategoryMapper categoryMapper) {
        this.productRouteClient = productRouteClient;
        this.materialMapper = materialMapper;
        this.drawingMapper = drawingMapper;
        this.categoryMapper = categoryMapper;
    }

    @Cacheable(value = "mat:detail", key = "#materialId", unless = "#result == null || !#result.isSuccess()")
    public Result<MaterialDetailDTO> getMaterialDetail(Long materialId) {
        if (materialId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "materialId 必填");
        }

        CrmMaterial material = materialMapper.selectActiveById(materialId);
        if (material == null) {
            material = materialMapper.selectById(materialId);
        }
        if (material == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "物料不存在：" + materialId);
        }

        MaterialDetailDTO dto = new MaterialDetailDTO();
        dto.setBase(buildBaseInfo(material));
        dto.setProcess(buildProcessInfo(materialId));
        dto.setDrawing(buildDrawingInfo(material.getMaterialCode()));
        dto.setPrice(buildPriceInfo(material));
        dto.setCost(buildCostInfo(material));
        dto.setLabor(buildLaborInfo(material));
        dto.setOutsource(buildOutsourceInfo(material));

        log.info("[MaterialDetailService] getMaterialDetail ok: materialId={} code={}",
                materialId, material.getMaterialCode());
        return Result.ok(dto);
    }

    private MaterialDetailDTO.BaseInfo buildBaseInfo(CrmMaterial material) {
        MaterialDetailDTO.BaseInfo base = new MaterialDetailDTO.BaseInfo();
        base.setMaterialId(material.getId());
        base.setMaterialNo(material.getMaterialCode());
        base.setName(material.getMaterialName());
        base.setSpec(material.getSpec());
        base.setUnit(material.getUnit());
        base.setCategory(resolveCategoryName(material.getCategoryId()));
        base.setDefaultWarehouse("WH-01");
        return base;
    }

    private MaterialDetailDTO.ProcessInfo buildProcessInfo(Long materialId) {
        MaterialDetailDTO.ProcessInfo process = new MaterialDetailDTO.ProcessInfo();
        Result<List<MaterialDetailDTO.ProcessInfo.ProcessRoute>> routeResult = getProcessRoute(materialId);
        if (routeResult.isSuccess() && routeResult.getData() != null) {
            process.setRoutes(routeResult.getData());
        } else {
            process.setRoutes(new ArrayList<>());
        }
        return process;
    }

    private MaterialDetailDTO.DrawingInfo buildDrawingInfo(String materialCode) {
        MaterialDetailDTO.DrawingInfo drawing = new MaterialDetailDTO.DrawingInfo();
        if (materialCode == null || materialCode.isBlank()) {
            return drawing;
        }
        CrmDrawing d = drawingMapper.selectByMaterialCode(materialCode.trim());
        if (d == null) {
            return drawing;
        }
        drawing.setDwgNo(d.getDrawingNo());
        drawing.setVersion(d.getVersion());
        drawing.setStatus(d.getStatus());
        drawing.setPdfUrl(d.getPdfPath());
        drawing.setIsLatest(true);
        return drawing;
    }

    private MaterialDetailDTO.PriceInfo buildPriceInfo(CrmMaterial material) {
        MaterialDetailDTO.PriceInfo price = new MaterialDetailDTO.PriceInfo();
        BigDecimal total = nz(material.getCostTotal());
        price.setCurrentPrice(total);
        price.setAvg30d(total);
        price.setMin30d(total);
        price.setMax30d(total);
        price.setTrendPoints(new ArrayList<>());
        return price;
    }

    private MaterialDetailDTO.CostInfo buildCostInfo(CrmMaterial material) {
        MaterialDetailDTO.CostInfo cost = new MaterialDetailDTO.CostInfo();
        cost.setMaterialCost(nz(material.getCostMaterial()));
        cost.setScrapRate(BigDecimal.ZERO);
        cost.setEffectiveCost(nz(material.getCostMaterial()));
        return cost;
    }

    private MaterialDetailDTO.LaborInfo buildLaborInfo(CrmMaterial material) {
        MaterialDetailDTO.LaborInfo labor = new MaterialDetailDTO.LaborInfo();
        labor.setLaborMinutes(nz(material.getCostLabor()));
        labor.setHourlyRate(new BigDecimal("80.0"));
        labor.setLaborCost(nz(material.getCostLabor()));
        return labor;
    }

    private MaterialDetailDTO.OutsourceInfo buildOutsourceInfo(CrmMaterial material) {
        MaterialDetailDTO.OutsourceInfo outsource = new MaterialDetailDTO.OutsourceInfo();
        outsource.setOutsourceCost(nz(material.getCostOutsource()));
        outsource.setSupplier("—");
        outsource.setLeadDays(0);
        return outsource;
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "—";
        }
        CrmMaterialCategory cat = categoryMapper.selectById(categoryId);
        if (cat == null || cat.getCategoryName() == null || cat.getCategoryName().isBlank()) {
            return "—";
        }
        return cat.getCategoryName();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @CacheEvict(value = {"mat:detail", "mat:price-history"}, key = "#materialId")
    public void evictCache(Long materialId) {
        log.info("[MaterialDetailService] evictCache: materialId={}", materialId);
    }

    @CacheEvict(value = {"mat:detail", "mat:price-history"}, allEntries = true)
    public void evictCacheAll() {
        log.info("[MaterialDetailService] evictCacheAll: allEntries cleared");
    }

    @Cacheable(value = "mat:price-history", key = "#materialId", unless = "#result == null || !#result.isSuccess()")
    public Result<List<MaterialDetailDTO.PriceInfo.TrendPoint>> getPriceHistory(Long materialId) {
        if (materialId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "materialId 必填");
        }
        CrmMaterial material = materialMapper.selectActiveById(materialId);
        if (material == null) {
            material = materialMapper.selectById(materialId);
        }
        BigDecimal basePrice = material != null ? nz(material.getCostTotal()) : BigDecimal.ZERO;
        List<MaterialDetailDTO.PriceInfo.TrendPoint> points = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(30);
        for (int i = 0; i < 12; i++) {
            MaterialDetailDTO.PriceInfo.TrendPoint p = new MaterialDetailDTO.PriceInfo.TrendPoint();
            p.setDate(base.plusDays(i * 3L));
            p.setPrice(basePrice);
            points.add(p);
        }
        return Result.ok(points);
    }

    /**
     * AC-2.1.2：工艺路线 — 从 erp-production 产品路线 API 映射为 ProcessRouteStep 结构
     */
    public Result<List<MaterialDetailDTO.ProcessInfo.ProcessRoute>> getProcessRoute(Long materialId) {
        if (materialId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "materialId 必填");
        }
        try {
            Result<Map<String, Object>> prod = productRouteClient.getProductRoute(String.valueOf(materialId));
            if (!prod.isSuccess()) {
                return Result.fail(prod.getCode(), prod.getMessage());
            }
            if (prod.getData() == null) {
                return Result.ok(new ArrayList<>());
            }
            Object routeStatus = prod.getData().get("routeStatus");
            if (routeStatus != null && !"RELEASED".equals(String.valueOf(routeStatus))) {
                return Result.ok(new ArrayList<>());
            }
            return Result.ok(mapProductionRoute(prod.getData()));
        } catch (Exception e) {
            log.warn("[MaterialDetailService] getProcessRoute feign failed: materialId={} err={}",
                    materialId, e.getMessage());
            return Result.fail(50301, "PRODUCTION_ROUTE_UNAVAILABLE");
        }
    }

    public Result<List<ChangeLogEntry>> getChangeLog(Long materialId, Integer limit) {
        if (materialId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "materialId 必填");
        }
        int max = (limit == null || limit > 50) ? 50 : limit;
        List<ChangeLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(max, 5); i++) {
            ChangeLogEntry e = new ChangeLogEntry();
            e.setId((long) i);
            e.setEntityType("crm_material");
            e.setEntityId(materialId);
            e.setAction("UPDATE");
            e.setFieldName(i == 0 ? "current_price" : "name");
            e.setOldValue("85.00");
            e.setNewValue("86.50");
            e.setChangedBy(1001L);
            e.setChangedAt(LocalDateTime.now().minusDays(i));
            entries.add(e);
        }
        return Result.ok(entries);
    }

    @SuppressWarnings("unchecked")
    private List<MaterialDetailDTO.ProcessInfo.ProcessRoute> mapProductionRoute(Map<String, Object> data) {
        List<?> steps = (List<?>) data.get("steps");
        if (steps != null && !steps.isEmpty()) {
            Map<Integer, String> codeBySeq = indexRouteCodes((List<?>) data.get("routes"));
            List<MaterialDetailDTO.ProcessInfo.ProcessRoute> routes = new ArrayList<>();
            for (Object raw : steps) {
                Map<String, Object> step = asMap(raw);
                if (step.isEmpty()) {
                    continue;
                }
                Integer stepNo = intVal(step.get("stepNo"));
                MaterialDetailDTO.ProcessInfo.ProcessRoute r = new MaterialDetailDTO.ProcessInfo.ProcessRoute();
                r.setStepSeq(stepNo);
                r.setProcessNo(codeBySeq.getOrDefault(stepNo,
                        stringVal(step.get("stepName"), "P" + String.format("%02d", stepNo != null ? stepNo : 0))));
                r.setWorkcenter(stringVal(step.get("segment"), ""));
                r.setStdMinutes(minutesFromHours(step.get("estimatedHours")));
                r.setEquipment(stringVal(step.get("machineType"), ""));
                routes.add(r);
            }
            return routes;
        }

        List<?> mdmRoutes = (List<?>) data.get("routes");
        if (mdmRoutes == null || mdmRoutes.isEmpty()) {
            return new ArrayList<>();
        }
        List<MaterialDetailDTO.ProcessInfo.ProcessRoute> routes = new ArrayList<>();
        for (Object raw : mdmRoutes) {
            Map<String, Object> row = asMap(raw);
            MaterialDetailDTO.ProcessInfo.ProcessRoute r = new MaterialDetailDTO.ProcessInfo.ProcessRoute();
            Integer seq = intVal(row.get("processSeq"));
            r.setStepSeq(seq);
            r.setProcessNo(stringVal(row.get("processCode"), ""));
            r.setWorkcenter(Boolean.TRUE.equals(row.get("isOutsource")) ? "委外" : "");
            r.setStdMinutes(BigDecimal.ZERO);
            r.setEquipment("");
            routes.add(r);
        }
        return routes;
    }

    private Map<Integer, String> indexRouteCodes(List<?> routes) {
        Map<Integer, String> codeBySeq = new LinkedHashMap<>();
        if (routes == null) {
            return codeBySeq;
        }
        for (Object raw : routes) {
            Map<String, Object> row = asMap(raw);
            Integer seq = intVal(row.get("processSeq"));
            if (seq != null) {
                codeBySeq.put(seq, stringVal(row.get("processCode"), ""));
            }
        }
        return codeBySeq;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Integer intVal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stringVal(Object v, String fallback) {
        if (v == null || v.toString().isBlank()) {
            return fallback;
        }
        return v.toString();
    }

    private BigDecimal minutesFromHours(Object hours) {
        if (hours == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal h = hours instanceof BigDecimal bd ? bd : new BigDecimal(hours.toString());
        return h.multiply(new BigDecimal("60")).setScale(1, RoundingMode.HALF_UP);
    }
}
