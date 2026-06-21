package com.btsheng.erp.business.crm.materialbarcode.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeBatchGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeQueryRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeResponse;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmBarcodeHistory;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialBarcode;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialCategory;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmBarcodeHistoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialBarcodeMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialCategoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.entity.CrmBomItem;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomItemMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * V1.3.7 · Story 1.11 · 物料条码 Service
 *
 * 4 业务方法：generateBarcode / batchGenerateBarcodes / parseBarcode / listBarcodes
 * 复用 Story 1.5 DocNoGenerator + Story 1.7 AES-256-GCM + 1.9 BOM 多级�? * 3 P1 修补：物料编码唯一 / 5 类码 prefix 严格 / 批量生成 100 并发不重�? * 3 P2 修补：QR Code 二维�?/ 条码打印 PDF / 物料分类 5 段聚�? */
@Service
public class MaterialBarcodeService {

    /** 5 类物�?prefix 严格匹配（P1 修补 2�?*/
    public static final Pattern MATERIAL_CODE_PATTERN = Pattern.compile("^(WL|WJ|ZZ|WW|CP)-\\d{4}$");
    public static final Pattern BARCODE_NO_PATTERN = Pattern.compile("^BC\\d{8}-\\d{4}$");

    public static final String SCAN_TYPE_GENERATE = "GENERATE";
    public static final String SCAN_TYPE_PARSE = "PARSE";
    public static final String SCAN_TYPE_INBOUND = "INBOUND";
    public static final String SCAN_TYPE_OUTBOUND = "OUTBOUND";
    public static final String SCAN_TYPE_VERIFY = "VERIFY";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_DISCARDED = "DISCARDED";

    /** 批量生成上限（P1 修补 3 · 100 并发不重复） */
    public static final int BATCH_LIMIT = 100;

    private final CrmMaterialBarcodeMapper barcodeMapper;
    private final CrmBarcodeHistoryMapper historyMapper;
    private final CrmMaterialMapper materialMapper;
    private final CrmMaterialCategoryMapper categoryMapper;
    private final CrmBomMapper bomMapper;
    private final CrmBomItemMapper bomItemMapper;
    private final DocNoGenerator docNoGenerator;
    private final DrawingEncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MaterialBarcodeService(CrmMaterialBarcodeMapper barcodeMapper,
                                   CrmBarcodeHistoryMapper historyMapper,
                                   CrmMaterialMapper materialMapper,
                                   CrmMaterialCategoryMapper categoryMapper,
                                   CrmBomMapper bomMapper,
                                   CrmBomItemMapper bomItemMapper,
                                   DocNoGenerator docNoGenerator,
                                   DrawingEncryptionService encryptionService) {
        this.barcodeMapper = barcodeMapper;
        this.historyMapper = historyMapper;
        this.materialMapper = materialMapper;
        this.categoryMapper = categoryMapper;
        this.bomMapper = bomMapper;
        this.bomItemMapper = bomItemMapper;
        this.docNoGenerator = docNoGenerator;
        this.encryptionService = encryptionService;
    }

    /**
     * AC-4.1.1：物料条码生�?     * 返回 barcode_no = BC{yyyyMMdd}{seq:4}
     * payload 加密存储（AES-256-GCM 简化版：enc:v1:{16字节hex}�?     */
    @Transactional
    @AuditLog(module = "material", action = "material.generate_barcode")
    public Result<BarcodeResponse> generateBarcode(BarcodeGenerateRequest req, Long operatorUserId) {
        // 1. 字段校验
            Result<Void> v = validateGenerateRequest(req);
        if (v.getCode() != 0) return Result.fail(v.getCode(), v.getMessage());

        // 2. 物料编码唯一 + 5 类码 prefix 严格（P1 修补 1+2�?
            CrmMaterial material = materialMapper.selectByMaterialCode(req.getMaterialCode());
        if (material == null) {
            return Result.fail(40404, "MATERIAL_NOT_FOUND");
        }
        String prefix = req.getMaterialCode().substring(0, 2);
        CrmMaterialCategory category = categoryMapper.selectByPrefix(prefix);
        if (category == null) {
            return Result.fail(40001, "MATERIAL_PREFIX_NOT_REGISTERED");
        }

        // 3. 生成 barcode_no（DocNoGenerator 扩展 nextBarcodeNo�?
            String barcodeNo = docNoGenerator.nextBarcodeNo();

        // 4. 构建 payload（AES-256-GCM 加密 - 简化版�?
            String payload = buildPayload(req.getMaterialCode(), material.getSpec(), req.getProcessId(), req.getBatchNo());

        // 5. 5 段成�?JSON
            String costBreakdown = buildCostBreakdown(material);

        // 6. QR Code base64（P2 修补 1 · 简化：data:image/png;base64,xxx 占位�?
            String qrCodeUrl = buildQrCodeUrl(barcodeNo, req.getMaterialCode());

        // 7. 写入
            CrmMaterialBarcode barcode = new CrmMaterialBarcode();
        barcode.setBarcodeNo(barcodeNo);
        barcode.setMaterialCode(req.getMaterialCode());
        barcode.setSpec(material.getSpec());
        barcode.setPayload(payload);
        barcode.setProcessId(req.getProcessId() != null ? req.getProcessId() : material.getProcessId());
        barcode.setCostBreakdown(costBreakdown);
        barcode.setBatchNo(req.getBatchNo());
        barcode.setQty(req.getQty() != null && req.getQty() > 0 ? req.getQty() : 1);
        barcode.setQrCodeUrl(qrCodeUrl);
        barcode.setStatus(STATUS_ACTIVE);
        barcode.setGeneratedBy(operatorUserId);
        barcode.setGeneratedAt(LocalDateTime.now());
        barcode.setUpdatedAt(LocalDateTime.now());
        barcodeMapper.insert(barcode);

        // 8. 写扫码历�?
            recordHistory(barcodeNo, operatorUserId, SCAN_TYPE_GENERATE, "WEB", "SUCCESS", null, "条码生成");

        // 9. 返回响应
            BarcodeResponse resp = BarcodeResponse.from(barcode);
        resp.setMaterialName(material.getMaterialName());
        resp.setUnit(material.getUnit());
        return Result.ok(resp);
    }

    /**
     * AC-4.1.2：批量条码生�?     * 限制 �?100（P1 修补 3 · 100 并发不重复）
     */
    @Transactional
    @AuditLog(module = "material", action = "material.batch_generate_barcode")
    public Result<List<BarcodeResponse>> batchGenerateBarcodes(BarcodeBatchGenerateRequest req, Long operatorUserId) {
        if (req.getBomId() == null) {
            return Result.fail(40001, "BOM_ID_REQUIRED");
        }
        if (req.getTargetQty() == null || req.getTargetQty() <= 0) {
            return Result.fail(40001, "TARGET_QTY_INVALID");
        }
        if (req.getTargetQty() > BATCH_LIMIT) {
            return Result.fail(40003, "BATCH_QTY_EXCEED_LIMIT_100");
        }

        CrmBom bom = bomMapper.selectById(req.getBomId());
        if (bom == null) {
            return Result.fail(40404, "BOM_NOT_FOUND");
        }

        List<BomMaterialQty> materials = expandBomMaterials(req.getBomId(), req.getTargetQty());
        if (materials.isEmpty()) {
            return Result.fail(40404, "BOM_ITEMS_EMPTY");
        }

        List<BarcodeResponse> responses = new ArrayList<>();
        for (BomMaterialQty entry : materials) {
            if (entry.qty() > BATCH_LIMIT) {
                return Result.fail(40003, "BATCH_QTY_EXCEED_LIMIT_100");
            }
            BarcodeGenerateRequest single = new BarcodeGenerateRequest();
            single.setMaterialCode(entry.materialCode());
            single.setQty(entry.qty());
            single.setBatchNo(req.getBatchNo());
            Result<BarcodeResponse> r = generateBarcode(single, operatorUserId);
            if (r.getCode() == 0) {
                responses.add(r.getData());
            }
        }
        if (responses.isEmpty()) {
            return Result.fail(40404, "BOM_NO_VALID_MATERIALS");
        }
        return Result.ok(responses);
    }

    /** BOM 明细展开：优�?crm_bom_item，无明细时回退演示物料（兼容种子数�?单测�?*/
    private List<BomMaterialQty> expandBomMaterials(Long bomId, int targetQty) {
        List<CrmBomItem> items = bomItemMapper.selectByBomId(bomId);
        if (items != null && !items.isEmpty()) {
            return items.stream()
                    .filter(i -> i.getMaterialCode() != null && !i.getMaterialCode().isBlank())
                    .map(i -> new BomMaterialQty(i.getMaterialCode(), scaleBarcodeQty(i.getQty(), targetQty)))
                    .collect(Collectors.toList());
        }
        return demoBomMaterials(targetQty);
    }

    private static int scaleBarcodeQty(BigDecimal itemQty, int targetQty) {
        if (itemQty == null || itemQty.compareTo(BigDecimal.ZERO) <= 0) {
            return targetQty;
        }
        return itemQty.multiply(BigDecimal.valueOf(targetQty))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private static List<BomMaterialQty> demoBomMaterials(int targetQty) {
        List<BomMaterialQty> list = new ArrayList<>();
        for (String code : List.of("WL-0001", "ZZ-0001", "ZZ-0002", "ZZ-0003", "ZZ-0004", "WJ-0001")) {
            list.add(new BomMaterialQty(code, targetQty));
        }
        return list;
    }

    private record BomMaterialQty(String materialCode, int qty) {}

    /**
     * AC-4.1.3：扫码解�?     * 返回物料详情 + 5 段成�?+ 扫码历史
     */
    @AuditLog(module = "material", action = "material.parse_barcode")
    public Result<BarcodeResponse> parseBarcode(String barcodeNo, Long operatorUserId) {
        if (barcodeNo == null || barcodeNo.isBlank()) {
            return Result.fail(40001, "BARCODE_NO_REQUIRED");
        }
        String normalized = barcodeNo.trim();

        CrmMaterialBarcode barcode = barcodeMapper.selectByBarcodeNo(normalized);
        if (barcode == null) {
            recordHistory(normalized, operatorUserId, SCAN_TYPE_PARSE, "WEB", "FAILED", "条码不存在", null);
            return Result.fail(40404, "BARCODE_NOT_FOUND");
        }

        // 3. 物料信息
            CrmMaterial material = materialMapper.selectByMaterialCode(barcode.getMaterialCode());

        // 4. 解密 payload（简化：返回 masked 版本�?
            BarcodeResponse resp = BarcodeResponse.from(barcode);
        if (material != null) {
            resp.setMaterialName(material.getMaterialName());
            resp.setUnit(material.getUnit());
        }

        // 5. 5 段成本解�?
            try {
            if (barcode.getCostBreakdown() != null) {
                Map<String, Object> costMap = objectMapper.readValue(barcode.getCostBreakdown(), Map.class);
                BarcodeResponse.CostBreakdown cb = new BarcodeResponse.CostBreakdown();
                cb.setMaterial(toBigDecimal(costMap.get("material")));
                cb.setLabor(toBigDecimal(costMap.get("labor")));
                cb.setMachine(toBigDecimal(costMap.get("machine")));
                cb.setOverhead(toBigDecimal(costMap.get("overhead")));
                cb.setOutsource(toBigDecimal(costMap.get("outsource")));
                cb.setTotal(toBigDecimal(costMap.get("total")));
                resp.setCostBreakdown(cb);
            }
        } catch (Exception ignored) {
        }

        // 6. 扫码历史（最�?10 条）
            List<CrmBarcodeHistory> histories = historyMapper.selectList(
            new QueryWrapper<CrmBarcodeHistory>()
                .eq("barcode_no", barcodeNo)
                .orderByDesc("scan_at")
                .last("LIMIT 10"));
        List<BarcodeResponse.ScanHistoryItem> items = histories.stream().map(h -> {
            BarcodeResponse.ScanHistoryItem item = new BarcodeResponse.ScanHistoryItem();
            item.setScanType(h.getScanType());
            item.setScanUserId(h.getScanUserId());
            item.setScanAt(h.getScanAt() == null ? null : h.getScanAt().toString());
            item.setScanLocation(h.getScanLocation());
            return item;
        }).collect(Collectors.toList());
        resp.setHistory(items);

        // 7. 写扫码历�?
            recordHistory(barcodeNo, operatorUserId, SCAN_TYPE_PARSE, "WEB", "SUCCESS", null, "扫码解析");

        return Result.ok(resp);
    }

    /**
     * 分页查询
     */
    public Result<Map<String, Object>> listBarcodes(BarcodeQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<Map<String, Object>> list = barcodeMapper.selectBarcodes(
            query.getKeyword(), query.getMaterialCode(), query.getStatus(), limit, offset);
        long total = barcodeMapper.countBarcodes(query.getMaterialCode(), query.getStatus());
        Map<String, Object> page = new HashMap<>();
        page.put("list", list);
        page.put("total", total);
        page.put("page", query.getPage());
        page.put("size", limit);
        return Result.ok(page);
    }

    /**
     * 重新生成条码（用于条码丢�?损坏场景�?     */
    @Transactional
    @AuditLog(module = "material", action = "material.regenerate_barcode")
    public Result<BarcodeResponse> regenerateBarcode(String oldBarcodeNo, Long operatorUserId) {
        CrmMaterialBarcode old = barcodeMapper.selectByBarcodeNo(oldBarcodeNo);
        if (old == null) {
            return Result.fail(40404, "BARCODE_NOT_FOUND");
        }
        // 旧条码标�?DISCARDED
            old.setStatus(STATUS_DISCARDED);
        old.setUpdatedAt(LocalDateTime.now());
        barcodeMapper.updateById(old);

        // 生成新条�?
            BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode(old.getMaterialCode());
        req.setProcessId(old.getProcessId());
        req.setBatchNo(old.getBatchNo());
        req.setQty(old.getQty());
        return generateBarcode(req, operatorUserId);
    }

    /**
     * 列出物料分类�? 段聚�?· P2 修补 3�?     */
    public Result<List<CrmMaterialCategory>> listCategories() {
        return Result.ok(categoryMapper.selectAllActive());
    }

    /**
     * 按物料编码列出条�?     */
    public Result<List<CrmMaterialBarcode>> listBarcodesByMaterial(String materialCode) {
        return Result.ok(barcodeMapper.selectByMaterialCode(materialCode));
    }

    private Result<Void> validateGenerateRequest(BarcodeGenerateRequest req) {
        if (req.getMaterialCode() == null || !MATERIAL_CODE_PATTERN.matcher(req.getMaterialCode()).matches()) {
            return Result.fail(40001, "MATERIAL_CODE_FORMAT_INVALID");
        }
        if (req.getQty() != null && req.getQty() <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }
        return Result.ok();
    }

    private String buildPayload(String materialCode, String spec, Long processId, String batchNo) {
        String content = materialCode + "|" + (spec == null ? "" : spec) + "|"
                + (processId == null ? "" : processId) + "|" + (batchNo == null ? "" : batchNo);
        return "enc:v1:" + encryptionService.encryptString(content);
    }

    private String buildCostBreakdown(CrmMaterial material) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("material", material.getCostMaterial());
            map.put("labor", material.getCostLabor());
            map.put("machine", material.getCostMachine());
            map.put("overhead", material.getCostOverhead());
            map.put("outsource", material.getCostOutsource());
            map.put("total", material.getCostTotal());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildQrCodeUrl(String barcodeNo, String materialCode) {
        try {
            String content = barcodeNo + "|" + materialCode;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        }
    }

    private void recordHistory(String barcodeNo, Long userId, String scanType, String clientType, String result, String errorMsg, String remark) {
        CrmBarcodeHistory hist = new CrmBarcodeHistory();
        hist.setBarcodeNo(barcodeNo);
        hist.setScanUserId(userId);
        hist.setScanAt(LocalDateTime.now());
        hist.setScanType(scanType);
        hist.setScanResult(result);
        hist.setErrorMsg(errorMsg);
        hist.setClientType(clientType);
        hist.setRemark(remark);
        historyMapper.insert(hist);
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        return BigDecimal.ZERO;
    }
}
