package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingSignature;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingSignatureMapper;
import com.btsheng.erp.core.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.4 · PDF 导出服务
 *
 * 1h 缓存模式（复用 Story 1.5）
 * 含：图号 + 版本 + 工艺路线 + 审批签字栏 + 签字扫描件（解密后嵌入）
 * 工艺路线 5 段成本聚合 hook（V1.3.4 闭环 · 留 1.9 BOM Story）
 */
@Slf4j
@Service
public class DrawingPdfExportService {

    private static final long CACHE_TTL_MS = 3600_000L; // 1h
            private final CrmDrawingMapper drawingMapper;
    private final CrmDrawingSignatureMapper signatureMapper;
    private final DrawingEncryptionService encryptionService;
    private final DrawingMinioFileService fileStorage;

    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public DrawingPdfExportService(CrmDrawingMapper drawingMapper,
                                    CrmDrawingSignatureMapper signatureMapper,
                                    DrawingEncryptionService encryptionService,
                                    DrawingMinioFileService fileStorage) {
        this.drawingMapper = drawingMapper;
        this.signatureMapper = signatureMapper;
        this.encryptionService = encryptionService;
        this.fileStorage = fileStorage;
    }

    /**
     * 导出 PDF（简化文本格式，OpenPDF 1.3.34 实际项目引入；本服务返回可嵌入文本）
     */
    public Result<byte[]> exportPdf(Long drawingId, String format) {
        // 1. 缓存命中
            CacheEntry cached = cache.get(drawingId);
        if (cached != null && (System.currentTimeMillis() - cached.ts) < CACHE_TTL_MS) {
            return Result.ok(cached.bytes);
        }

        CrmDrawing drawing = drawingMapper.selectById(drawingId);
        if (drawing == null) {
            return Result.fail(40404, "DRAWING_NOT_FOUND");
        }

        // 优先从 MinIO / 本地路径读取真实 PDF（FR-3-1-3 在线预览）
        if (drawing.getPdfPath() != null && !drawing.getPdfPath().isBlank()) {
            try {
                byte[] raw = fileStorage.readBytes(drawing.getPdfPath());
                if (DrawingMinioFileService.isPdf(raw)) {
                    cache.put(drawingId, new CacheEntry(raw, System.currentTimeMillis()));
                    return Result.ok(raw);
                }
            } catch (Exception e) {
                log.debug("MinIO PDF 读取失败，回退文本导出 drawingId={}: {}", drawingId, e.getMessage());
            }
        }

        // 2. 收集签字扫描件 + 解密
            List<CrmDrawingSignature> sigs = signatureMapper.selectByDrawingIdAndVersion(
            drawingId, drawing.getVersion());
        StringBuilder sigText = new StringBuilder();
        for (CrmDrawingSignature sig : sigs) {
            try {
                String decrypted = encryptionService.decryptString(sig.getSignatureImagePath());
                sigText.append("[签字人:").append(sig.getSignerUserId())
                       .append(" 签字:").append(decrypted).append("]\n");
            } catch (Exception e) {
                log.warn("签字扫描件解密失败: drawingId={}, signer={}", drawingId, sig.getSignerUserId());
            }
        }

        // 3. 工艺路线 5 段成本聚合 hook（V1.3.4 留 1.9 BOM）
            double totalCost = 0;
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> steps = m.readValue(drawing.getProcessRoute(), List.class);
            for (Map<String, Object> step : steps) {
                Object c = step.get("cost");
                if (c instanceof Number) totalCost += ((Number) c).doubleValue();
            }
        } catch (Exception ignored) {
        }

        // 4. 生成 PDF 文本（简化）
            String text = String.format("""
                =========================================
                  图号: %s  |  版本: %s  |  状态: %s
                  标题: %s
                  物料编码: %s  |  FA件: %s  |  新品: %s
                  创建时间: %s
                =========================================
                工艺路线（5 段成本聚合）:
                  %s
                  总成本: %.2f 元
                =========================================
                审批签字栏:
                """,
                drawing.getDrawingNo(), drawing.getVersion(), drawing.getStatus(),
                drawing.getTitle(), drawing.getMaterialCode(),
                drawing.getIsFa(), drawing.getIsNew(),
                drawing.getCreatedAt() == null ? "" : drawing.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                drawing.getProcessRoute(), totalCost)
                + sigText.toString()
                + """
                =========================================
                导出时间: """ + LocalDateTime.now() + """

                """;

        byte[] pdfBytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 5. 写缓存
            cache.put(drawingId, new CacheEntry(pdfBytes, System.currentTimeMillis()));
        return Result.ok(pdfBytes);
    }

    /**
     * 工艺路线 5 段成本聚合（V1.3.4 闭环 hook · 留 1.9 BOM Story）
     */
    public Result<Map<String, Object>> aggregateProcessRouteCost(String processRouteJson) {
        Map<String, Object> result = new HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> steps = m.readValue(processRouteJson, List.class);
            double total = 0;
            for (Map<String, Object> step : steps) {
                Object c = step.get("cost");
                if (c instanceof Number) total += ((Number) c).doubleValue();
            }
            result.put("stepCount", steps.size());
            result.put("totalCost", total);
            result.put("hook", "V1.3.4-cost-aggregator-ready-for-1.9-bom");
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(40001, "PROCESS_ROUTE_INVALID");
        }
    }

    private static class CacheEntry {
        final byte[] bytes;
        final long ts;
        CacheEntry(byte[] bytes, long ts) { this.bytes = bytes; this.ts = ts; }
    }
}
