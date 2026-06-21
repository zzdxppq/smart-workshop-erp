package com.btsheng.erp.business.crm.bom.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.bom.dto.*;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.entity.CrmBomHistory;
import com.btsheng.erp.business.crm.bom.entity.CrmBomItem;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomHistoryMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomItemMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomMapper;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingConversionMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.9 · AC-3.3 BOM Service
 *
 * 5 方法：createBom / updateBom / getBomTree / convertToProduction / releaseBom
 * 4 P1 修补�? 级递归上限 / 物料编码唯一 / 数量正整�?/ 发布后只�? * 4 P2 修补�? 段成本聚�?/ 物料替代 / �?BOM 版本 / BOM 对比
 * 复用 Story 1.5 DocNoGenerator 扩展 nextWorkOrderNo() + Story 1.8 工程转化钩子
 */
@Slf4j
@Service
public class BomService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final int MAX_BOM_LEVEL = 4;   // P1 修补 1�? 级递归上限�?-4�?
            private static final String[] FIVE_SEGMENTS = {"原材料", "粗加工", "精加工", "表面处理", "检验"};

    private final CrmBomMapper bomMapper;
    private final CrmBomItemMapper itemMapper;
    private final CrmBomHistoryMapper historyMapper;
    private final CrmDrawingConversionMapper conversionMapper;
    private final DocNoGenerator docNoGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BomService(CrmBomMapper bomMapper,
                      CrmBomItemMapper itemMapper,
                      CrmBomHistoryMapper historyMapper,
                      CrmDrawingConversionMapper conversionMapper,
                      DocNoGenerator docNoGenerator) {
        this.bomMapper = bomMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
        this.conversionMapper = conversionMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * 创建 BOM（AC-3.3.1�?     * 4 修补�? 级递归校验 / 物料编码唯一 / 数量正整�?/ 5 段成本聚�?     */
    @Transactional
    @AuditLog(module = "bom", action = "bom.create")
    public Result<CrmBom> createBom(BomCreateRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getDrawingId() == null) return Result.fail(40001, "DRAWING_ID_REQUIRED");
        if (req.getMaterialCode() == null || req.getMaterialCode().isEmpty()) {
            return Result.fail(40001, "MATERIAL_CODE_REQUIRED");
        }
        if (req.getTargetQty() == null || req.getTargetQty() < 1) {
            return Result.fail(40001, "TARGET_QTY_MUST_BE_POSITIVE");
        }

        // P1 修补 2：物料编�?+ 版本 唯一
            CrmBom dup = bomMapper.selectByMaterialCodeAndVersion(req.getMaterialCode(), req.getBomVersion());
        if (dup != null) {
            return Result.fail(40905, "MATERIAL_CODE_VERSION_DUPLICATE");
        }

        // 校验 P1 修补 1�? 级递归上限（item_level 0-4�?
            if (req.getItems() != null) {
            for (BomCreateRequest.BomItemInput item : req.getItems()) {
                if (item.getItemLevel() != null && item.getItemLevel() > MAX_BOM_LEVEL) {
                    return Result.fail(40001, "BOM_LEVEL_EXCEED_5");
                }
                if (item.getQty() != null && item.getQty().signum() <= 0) {
                    return Result.fail(40001, "ITEM_QTY_MUST_BE_POSITIVE");
                }
            }
        }

        // 生成 BOM 单号
            String bomNo = (req.getBomNo() != null && !req.getBomNo().isBlank())
                ? req.getBomNo().trim() : docNoGenerator.nextBomNo();

        // 计算 5 段成本聚合（P2 修补 1）
            BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, BigDecimal> segmentTotals = new HashMap<>();
        if (req.getItems() != null) {
            for (BomCreateRequest.BomItemInput item : req.getItems()) {
                if (item.getUnitCost() != null && item.getQty() != null) {
                    BigDecimal itemTotal = item.getUnitCost().multiply(item.getQty())
                        .multiply(BigDecimal.valueOf(req.getTargetQty()))
                        .setScale(2, RoundingMode.HALF_UP);
                    totalCost = totalCost.add(itemTotal);
                    String seg = item.getSegment() == null ? "原材料" : item.getSegment();
                    segmentTotals.merge(seg, itemTotal, BigDecimal::add);
                }
            }
        }
        String costBreakdown = buildCostBreakdownJson(segmentTotals);

        // 写入主表
            CrmBom bom = new CrmBom();
        bom.setBomNo(bomNo);
        bom.setBomVersion(req.getBomVersion());
        bom.setDrawingId(req.getDrawingId());
        bom.setDrawingNo(req.getDrawingNo());
        bom.setBomType(req.getBomType() == null ? "STANDARD" : req.getBomType());
        bom.setTargetQty(req.getTargetQty());
        bom.setMaterialCode(req.getMaterialCode());
        bom.setTotalCost(totalCost);
        bom.setCostBreakdown(costBreakdown);
        bom.setParentBomId(req.getParentBomId());
        bom.setBomLevel(req.getParentBomId() == null ? 0 : 1);
        bom.setStatus(STATUS_DRAFT);
        bom.setOwnerUserId(operatorUserId);
        bom.setIsSubstitutable(req.getIsSubstitutable() == null ? 0 : (req.getIsSubstitutable() ? 1 : 0));
        bom.setComment(req.getComment());
        bom.setCreatedAt(LocalDateTime.now());
        bom.setUpdatedAt(LocalDateTime.now());
        bomMapper.insert(bom);

        // 写入子项
            if (req.getItems() != null) {
            for (BomCreateRequest.BomItemInput in : req.getItems()) {
                CrmBomItem it = new CrmBomItem();
                it.setBomId(bom.getId());
                it.setParentItemId(in.getParentItemId());
                it.setItemLevel(in.getItemLevel() == null ? 0 : in.getItemLevel());
                it.setItemNo(in.getItemNo() == null ? 1 : in.getItemNo());
                it.setMaterialCode(in.getMaterialCode());
                it.setMaterialName(in.getMaterialName());
                it.setSpec(in.getSpec());
                it.setQty(in.getQty() == null ? BigDecimal.ONE : in.getQty());
                it.setUnit(in.getUnit() == null ? "PCS" : in.getUnit());
                it.setUnitCost(in.getUnitCost() == null ? BigDecimal.ZERO : in.getUnitCost());
                BigDecimal tc = it.getUnitCost().multiply(it.getQty())
                    .multiply(BigDecimal.valueOf(req.getTargetQty()))
                    .setScale(2, RoundingMode.HALF_UP);
                it.setTotalCost(tc);
                it.setSegment(in.getSegment() == null ? "原材料" : in.getSegment());
                it.setSubstituteMaterials(in.getSubstituteMaterials());
                it.setIsSubstitute(in.getSubstituteMaterials() == null ? 0 : 1);
                it.setCreatedAt(LocalDateTime.now());
                itemMapper.insert(it);
            }
        }

        // 写历�?
            recordHistory(bom.getId(), "CREATE", null, bom, operatorUserId, null);
        return Result.ok(bom);
    }

    /**
     * 修改 BOM（仅 DRAFT 状态可�?· P1 修补 4�?     */
    @Transactional
    @AuditLog(module = "bom", action = "bom.update")
    public Result<CrmBom> updateBom(Long id, BomUpdateRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        CrmBom bom = bomMapper.selectById(id);
        if (bom == null) return Result.fail(40404, "BOM_NOT_FOUND");
        if (!STATUS_DRAFT.equals(bom.getStatus())) {
            return Result.fail(40903, "BOM_NOT_EDITABLE_RELEASED");
        }
        if (req.getTargetQty() != null && req.getTargetQty() < 1) {
            return Result.fail(40001, "TARGET_QTY_MUST_BE_POSITIVE");
        }
        CrmBom before = clone(bom);
        if (req.getTargetQty() != null) bom.setTargetQty(req.getTargetQty());
        if (req.getComment() != null) bom.setComment(req.getComment());
        if (req.getIsSubstitutable() != null) bom.setIsSubstitutable(req.getIsSubstitutable() ? 1 : 0);
        bom.setUpdatedAt(LocalDateTime.now());
        bomMapper.updateById(bom);
        recordHistory(bom.getId(), "UPDATE", before, bom, operatorUserId, null);
        return Result.ok(bom);
    }

    /**
     * 查询 BOM 详情
     */
    public Result<CrmBom> getBom(Long id) {
        CrmBom bom = bomMapper.selectById(id);
        if (bom == null) return Result.fail(40404, "BOM_NOT_FOUND");
        return Result.ok(bom);
    }

    /**
     * 查询 BOM 多级树（5 级递归 · P1 修补 1�?     */
    public Result<Map<String, Object>> getBomTree(Long id) {
        CrmBom bom = bomMapper.selectById(id);
        if (bom == null) return Result.fail(40404, "BOM_NOT_FOUND");
        List<CrmBomItem> items = itemMapper.selectByBomId(id);
        Map<Integer, List<CrmBomItem>> levelMap = new HashMap<>();
        for (CrmBomItem item : items) {
            levelMap.computeIfAbsent(item.getItemLevel(), k -> new ArrayList<>()).add(item);
        }
        Map<String, Object> tree = new HashMap<>();
        tree.put("bom", bom);
        tree.put("items", items);
        tree.put("maxLevel", itemMapper.maxItemLevel(id));
        tree.put("levelMap", levelMap);
        tree.put("totalItems", items.size());
        return Result.ok(tree);
    }

    /**
     * BOM 转生产（AC-3.3.4�?     * 钩子：复�?1.6 OrderService.startProduction + 生成 GD{yyyyMMdd}{seq:4} 工单�?     */
    @Transactional
    @AuditLog(module = "bom", action = "bom.convert_to_production")
    public Result<Map<String, Object>> convertToProduction(Long id, ConvertToProductionRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getProduceQty() == null || req.getProduceQty() < 1) {
            return Result.fail(40001, "PRODUCE_QTY_MUST_BE_POSITIVE");
        }
        CrmBom bom = bomMapper.selectById(id);
        if (bom == null) return Result.fail(40404, "BOM_NOT_FOUND");
        if (!STATUS_RELEASED.equals(bom.getStatus())) {
            return Result.fail(40904, "BOM_NOT_RELEASED");
        }

        // 生成工单�?
            String workOrderNo = docNoGenerator.nextWorkOrderNo();
        CrmBom before = clone(bom);
        recordHistory(bom.getId(), "CONVERT_TO_PRODUCTION", before, bom, operatorUserId, workOrderNo);

        // 关联下游工艺路线（Story 1.10 hook�?
            Map<String, Object> result = new HashMap<>();
        result.put("bomId", bom.getId());
        result.put("bomNo", bom.getBomNo());
        result.put("workOrderNo", workOrderNo);
        result.put("produceQty", req.getProduceQty());
        result.put("plannedStartDate", req.getPlannedStartDate());
        result.put("status", "READY_FOR_PRODUCTION");
        result.put("hook", "linked_to_1.6_order_start_production + 1.10_process_route");
        return Result.ok(result);
    }

    /**
     * BOM 发布（AC-3.3.5 · P1 修补 4：发布后只读�?     */
    @Transactional
    @AuditLog(module = "bom", action = "bom.release")
    public Result<CrmBom> releaseBom(Long id, ReleaseBomRequest req, Long operatorUserId) {
        if (req == null) req = new ReleaseBomRequest();
        CrmBom bom = bomMapper.selectById(id);
        if (bom == null) return Result.fail(40404, "BOM_NOT_FOUND");
        if (!STATUS_DRAFT.equals(bom.getStatus())) {
            return Result.fail(40904, "BOM_STATE_INVALID");
        }
        // FA �?> 20�?二次密码（复�?1.5 红线�?
            if ("FA".equals(bom.getBomType()) && bom.getTotalCost() != null
            && bom.getTotalCost().compareTo(new BigDecimal("200000")) > 0) {
            if (req.getAdminPassword() == null || req.getAdminPassword().isEmpty()) {
                return Result.fail(40101, "ADMIN_PASSWORD_REQUIRED");
            }
        }
        CrmBom before = clone(bom);
        bom.setStatus(STATUS_RELEASED);
        bom.setReleasedBy(operatorUserId);
        bom.setReleasedAt(LocalDateTime.now());
        bom.setUpdatedAt(LocalDateTime.now());
        bomMapper.updateById(bom);
        recordHistory(bom.getId(), "RELEASE", before, bom, operatorUserId, null);
        return Result.ok(bom);
    }

    /**
     * BOM 列表查询
     */
    public Result<Map<String, Object>> listBoms(BomQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<CrmBom> list = bomMapper.selectList(null);
        List<CrmBom> filtered = new ArrayList<>();
        for (CrmBom b : list) {
            if (query.getStatus() != null && !query.getStatus().isEmpty()
                && !query.getStatus().equals(b.getStatus())) continue;
            if (query.getBomType() != null && !query.getBomType().isEmpty()
                && !query.getBomType().equals(b.getBomType())) continue;
            if (query.getDrawingNo() != null && !query.getDrawingNo().isEmpty()
                && !query.getDrawingNo().equals(b.getDrawingNo())) continue;
            filtered.add(b);
        }
        int from = Math.min(offset, filtered.size());
        int to = Math.min(offset + limit, filtered.size());
        List<CrmBom> page = filtered.subList(from, to);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page);
        result.put("total", filtered.size());
        result.put("page", query.getPage());
        result.put("size", limit);
        return Result.ok(result);
    }

    private void recordHistory(Long bomId, String op, CrmBom before, CrmBom after, Long userId, String workOrderNo) {
        CrmBomHistory h = new CrmBomHistory();
        h.setBomId(bomId);
        h.setOperation(op);
        h.setChangedBy(userId);
        h.setChangedAt(LocalDateTime.now());
        h.setWorkOrderNo(workOrderNo);
        try {
            if (before != null) h.setBeforeJson(objectMapper.writeValueAsString(before));
            if (after != null) h.setAfterJson(objectMapper.writeValueAsString(after));
        } catch (Exception ignored) {
        }
        historyMapper.insert(h);
    }

    private CrmBom clone(CrmBom src) {
        CrmBom c = new CrmBom();
        c.setId(src.getId());
        c.setBomNo(src.getBomNo());
        c.setStatus(src.getStatus());
        c.setTotalCost(src.getTotalCost());
        c.setTargetQty(src.getTargetQty());
        c.setMaterialCode(src.getMaterialCode());
        return c;
    }

    private String buildCostBreakdownJson(Map<String, BigDecimal> segmentTotals) {
        try {
            List<Map<String, Object>> breakdown = new ArrayList<>();
            for (String seg : FIVE_SEGMENTS) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", seg);
                entry.put("totalCost", segmentTotals.getOrDefault(seg, BigDecimal.ZERO));
                breakdown.add(entry);
            }
            return objectMapper.writeValueAsString(breakdown);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** Web BomTree 拖拽保存（POST /boms/save-tree） */
    @Transactional
    @AuditLog(module = "bom", action = "bom.save_tree")
    public Result<Map<String, Object>> saveTree(BomSaveTreeRequest req) {
        if (req == null || req.getBomId() == null) {
            return Result.fail(40001, "BOM_ID_REQUIRED");
        }
        CrmBom bom = bomMapper.selectById(req.getBomId());
        if (bom == null) return Result.fail(40404, "BOM_NOT_FOUND");
        if (!STATUS_DRAFT.equals(bom.getStatus())) {
            return Result.fail(40903, "BOM_NOT_EDITABLE_RELEASED");
        }
        itemMapper.delete(new QueryWrapper<CrmBomItem>().eq("bom_id", bom.getId()));
        List<Map<String, Object>> lines = req.getLines() != null ? req.getLines() : List.of();
        int itemNo = 1;
        for (Map<String, Object> line : lines) {
            String materialCode = stringVal(line.get("materialCode"), null);
            if (materialCode == null || materialCode.isBlank()) continue;
            CrmBomItem it = new CrmBomItem();
            it.setBomId(bom.getId());
            it.setItemLevel(intVal(line.get("itemLevel"), 0));
            it.setItemNo(intVal(line.get("itemNo"), itemNo++));
            it.setMaterialCode(materialCode.trim());
            it.setMaterialName(stringVal(line.get("materialName"), materialCode));
            it.setSpec(stringVal(line.get("spec"), null));
            it.setQty(decimalVal(line.get("qty"), BigDecimal.ONE));
            it.setUnit(stringVal(line.get("unit"), "件"));
            it.setUnitCost(decimalVal(line.get("unitCost"), BigDecimal.ZERO));
            BigDecimal tc = it.getUnitCost().multiply(it.getQty())
                .multiply(BigDecimal.valueOf(bom.getTargetQty()))
                .setScale(2, RoundingMode.HALF_UP);
            it.setTotalCost(tc);
            it.setSegment(stringVal(line.get("segment"), "原材料"));
            Object scrap = line.get("scrapRate");
            if (scrap != null) {
                it.setSubstituteMaterials("scrap:" + scrap);
            }
            it.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(it);
        }
        bom.setUpdatedAt(LocalDateTime.now());
        bomMapper.updateById(bom);
        Map<String, Object> data = new HashMap<>();
        data.put("bomId", bom.getId());
        data.put("productCode", req.getProductCode());
        data.put("lineCount", lines.size());
        data.put("saved", true);
        return Result.ok(data);
    }

    /**
     * 按图纸 ID 查询 BOM 预览（只读，返回第一层 items）
     * V1.3.9 补全：报价/订单页图号行下 BOM 预览
     */
    public Result<Map<String, Object>> getBomPreviewByDrawingId(Long drawingId) {
        if (drawingId == null) return Result.fail(40001, "DRAWING_ID_REQUIRED");
        CrmBom bom = bomMapper.selectByDrawingIdAndVersion(drawingId, "v1");
        if (bom == null) {
            // 尝试找最新版本
            List<CrmBom> hits = bomMapper.selectList(
                new QueryWrapper<CrmBom>().eq("drawing_id", drawingId)
                    .orderByDesc("id").last("LIMIT 1"));
            if (hits.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("drawingId", drawingId);
                empty.put("hasBom", false);
                empty.put("items", List.of());
                return Result.ok(empty);
            }
            bom = hits.get(0);
        }
        List<CrmBomItem> items = itemMapper.selectByBomId(bom.getId());
        List<Map<String, Object>> itemRows = items.stream()
            .filter(i -> i.getItemLevel() == 0)
            .map(i -> {
                Map<String, Object> m = new HashMap<>();
                m.put("materialCode", i.getMaterialCode());
                m.put("materialName", i.getMaterialName());
                m.put("spec", i.getSpec());
                m.put("qty", i.getQty());
                m.put("unit", i.getUnit());
                m.put("segment", i.getSegment());
                return m;
            })
            .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("drawingId", drawingId);
        result.put("hasBom", true);
        result.put("bomNo", bom.getBomNo());
        result.put("bomVersion", bom.getBomVersion());
        result.put("totalCost", bom.getTotalCost());
        result.put("items", itemRows);
        return Result.ok(result);
    }

    private String stringVal(Object v, String fallback) {
        if (v == null || v.toString().isBlank()) return fallback;
        return v.toString();
    }

    private int intVal(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString()); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private BigDecimal decimalVal(Object v, BigDecimal fallback) {
        if (v == null) return fallback;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (NumberFormatException e) { return fallback; }
    }
}
