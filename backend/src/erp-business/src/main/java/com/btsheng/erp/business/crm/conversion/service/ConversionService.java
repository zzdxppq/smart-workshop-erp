package com.btsheng.erp.business.crm.conversion.service;

import com.btsheng.erp.business.crm.bom.dto.BomCreateRequest;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.service.BomService;
import com.btsheng.erp.business.crm.conversion.dto.ConversionQueryRequest;
import com.btsheng.erp.business.crm.conversion.dto.ConversionRequest;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
import com.btsheng.erp.business.crm.conversion.entity.CrmEngineerWorkload;
import com.btsheng.erp.business.crm.conversion.mapper.CrmDrawingConversionMapper;
import com.btsheng.erp.business.crm.conversion.mapper.CrmEngineerWorkloadMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingPdfExportService;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.service.MaterialMasterEnsureService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.2 工程转化 Service
 */
@Slf4j
@Service
public class ConversionService {

    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_CONVERTED = "CONVERTED";

    private static final String[] FIVE_SEGMENT_NAMES = {"原材料", "粗加工", "精加工", "表面处理", "检验"};

    private final CrmDrawingMapper drawingMapper;
    private final CrmDrawingConversionMapper conversionMapper;
    private final CrmEngineerWorkloadMapper workloadMapper;
    private final DrawingPdfExportService pdfExportService;
    private final DocNoGenerator docNoGenerator;
    private final MaterialMasterEnsureService materialMasterEnsureService;
    private final BomService bomService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ConversionService(CrmDrawingMapper drawingMapper,
                             CrmDrawingConversionMapper conversionMapper,
                             CrmEngineerWorkloadMapper workloadMapper,
                             DrawingPdfExportService pdfExportService,
                             DocNoGenerator docNoGenerator,
                             MaterialMasterEnsureService materialMasterEnsureService,
                             BomService bomService) {
        this.drawingMapper = drawingMapper;
        this.conversionMapper = conversionMapper;
        this.workloadMapper = workloadMapper;
        this.pdfExportService = pdfExportService;
        this.docNoGenerator = docNoGenerator;
        this.materialMasterEnsureService = materialMasterEnsureService;
        this.bomService = bomService;
    }

    @Transactional
    @AuditLog(module = "drawing", action = "drawing.convert")
    public Result<CrmDrawingConversion> convertDrawing(Long drawingId, ConversionRequest req, Long operatorUserId) {
        if (req == null) return Result.fail(40001, "REQUEST_BODY_INVALID");
        if (req.getTargetQty() == null || req.getTargetQty() < 1) {
            return Result.fail(40001, "TARGET_QTY_MUST_BE_POSITIVE");
        }
        if (req.getBomType() == null || req.getBomType().isEmpty()) {
            req.setBomType("STANDARD");
        }

        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) return Result.fail(40404, "DRAWING_NOT_FOUND");
        if (!STATUS_RELEASED.equals(drawing.getStatus())) {
            return Result.fail(40904, "DRAWING_STATE_NOT_RELEASED");
        }

        CrmDrawingConversion existing = conversionMapper.selectByDrawingIdAndVersion(drawingId, drawing.getVersion());
        if (existing != null) {
            return Result.fail(40905, "CONVERSION_ALREADY_EXISTS");
        }

        Result<Map<String, Object>> costHook = pdfExportService.aggregateProcessRouteCost(drawing.getProcessRoute());
        if (costHook.getCode() != 0) {
            return Result.fail(costHook.getCode(), costHook.getMessage());
        }
        Object costData = costHook.getData();
        BigDecimal totalCost = BigDecimal.ZERO;
        List<Map<String, Object>> steps = new ArrayList<>();
        if (costData instanceof Map) {
            Object tc = ((Map<?, ?>) costData).get("totalCost");
            if (tc instanceof Number) {
                totalCost = BigDecimal.valueOf(((Number) tc).doubleValue());
            }
        }
        String costBreakdown = buildCostBreakdown(steps, req.getTargetQty());

        String bomNo = docNoGenerator.nextBomNo();

        // 生成 WL 料号并绑定图纸
        String materialCode = drawing.getMaterialCode();
        if (materialCode == null || materialCode.isBlank()) {
            materialCode = docNoGenerator.nextMaterialCode();
        }
        String title = drawing.getTitle() != null ? drawing.getTitle() : drawing.getDrawingNo();
        CrmMaterial material = materialMasterEnsureService.ensureFromDrawing(materialCode, title);
        if (material == null) {
            return Result.fail(50001, "MATERIAL_CREATE_FAILED");
        }

        drawing.setMaterialCode(materialCode);
        drawing.setStatus(STATUS_CONVERTED);
        drawing.setUpdatedAt(LocalDateTime.now());
        drawingMapper.updateById(drawing);

        // 创建 BOM 实体（DRAFT）
        BomCreateRequest bomReq = new BomCreateRequest();
        bomReq.setDrawingId(drawingId);
        bomReq.setDrawingNo(drawing.getDrawingNo());
        bomReq.setBomNo(bomNo);
        bomReq.setBomType(req.getBomType());
        bomReq.setTargetQty(req.getTargetQty());
        bomReq.setMaterialCode(materialCode);
        bomReq.setComment(req.getComment());
        Result<CrmBom> bomResult = bomService.createBom(bomReq, operatorUserId);
        if (!bomResult.isSuccess() || bomResult.getData() == null) {
            return Result.fail(bomResult.getCode(), bomResult.getMessage());
        }
        CrmBom bom = bomResult.getData();

        String factoryDrawingNo = materialCode + "-" + capitalizeVersion(drawing.getVersion());

        CrmDrawingConversion conv = new CrmDrawingConversion();
        conv.setDrawingId(drawingId);
        conv.setDrawingNo(drawing.getDrawingNo());
        conv.setLockedVersion(drawing.getVersion());
        conv.setBomNo(bomNo);
        conv.setBomType(req.getBomType());
        conv.setTargetQty(req.getTargetQty());
        conv.setTotalCost(totalCost);
        conv.setEngineerUserId(operatorUserId);
        conv.setEngineerName(req.getEngineerName() == null ? "工程师" + operatorUserId : req.getEngineerName());
        conv.setStatus(STATUS_CONVERTED);
        conv.setProcessRouteSnapshot(drawing.getProcessRoute());
        conv.setCostBreakdown(costBreakdown);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conversionMapper.insert(conv);

        conv.setMaterialCode(materialCode);
        conv.setBomId(bom.getId());
        conv.setFactoryDrawingNo(factoryDrawingNo);

        incrementEngineerWorkload(operatorUserId, conv.getEngineerName(), LocalDate.now(), 0, 1);

        log.info("工程转化成功: drawingId={}, materialCode={}, bomId={}, bomNo={}",
                drawingId, materialCode, bom.getId(), bomNo);
        return Result.ok(conv);
    }

    private static String capitalizeVersion(String version) {
        if (version == null || version.isBlank()) return "V1.0";
        String v = version.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            return "V" + v.substring(1);
        }
        return "V" + v;
    }

    public Result<CrmDrawingConversion> getConversionById(Long id) {
        CrmDrawingConversion conv = conversionMapper.selectById(id);
        if (conv == null) return Result.fail(40404, "CONVERSION_NOT_FOUND");
        return Result.ok(conv);
    }

    public Result<Map<String, Object>> listConversions(ConversionQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<CrmDrawingConversion> list = conversionMapper.selectConvertedList(limit, offset);
        Map<String, Object> page = new HashMap<>();
        page.put("list", list);
        page.put("total", list.size());
        page.put("page", query.getPage());
        page.put("size", limit);
        return Result.ok(page);
    }

    private String buildCostBreakdown(List<Map<String, Object>> steps, int targetQty) {
        try {
            List<Map<String, Object>> breakdown = new ArrayList<>();
            int stepCount = steps == null ? 0 : steps.size();
            for (int i = 0; i < FIVE_SEGMENT_NAMES.length; i++) {
                Map<String, Object> seg = new HashMap<>();
                seg.put("name", FIVE_SEGMENT_NAMES[i]);
                if (i < stepCount) {
                    Map<String, Object> step = steps.get(i);
                    Object c = step.get("cost");
                    double unit = c instanceof Number ? ((Number) c).doubleValue() : 0;
                    seg.put("unitCost", unit);
                    seg.put("totalCost", BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(targetQty)).setScale(2, RoundingMode.HALF_UP));
                    seg.put("stepName", step.get("name"));
                } else {
                    seg.put("unitCost", 0);
                    seg.put("totalCost", 0);
                }
                breakdown.add(seg);
            }
            return objectMapper.writeValueAsString(breakdown);
        } catch (Exception e) {
            log.warn("构建 5 段成本明细失败: {}", e.getMessage());
            return "[]";
        }
    }

    private void incrementEngineerWorkload(Long userId, String userName, LocalDate date, int deltaAnnot, int deltaConv) {
        try {
            CrmEngineerWorkload load = workloadMapper.selectByUserAndDate(userId, date);
            if (load == null) {
                load = new CrmEngineerWorkload();
                load.setUserId(userId);
                load.setUserName(userName);
                load.setWorkDate(date);
                load.setAnnotationCount(deltaAnnot);
                load.setConversionCount(deltaConv);
                load.setDrawingCreatedCount(0);
                workloadMapper.insert(load);
            } else {
                load.setAnnotationCount(load.getAnnotationCount() + deltaAnnot);
                load.setConversionCount(load.getConversionCount() + deltaConv);
                workloadMapper.updateById(load);
            }
        } catch (Exception e) {
            log.warn("工程师工作量统计失败: userId={}, err={}", userId, e.getMessage());
        }
    }
}
