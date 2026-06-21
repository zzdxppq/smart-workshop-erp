package com.btsheng.erp.business.crm.conversion.controller;

import com.btsheng.erp.business.crm.conversion.dto.AnnotationRequest;
import com.btsheng.erp.business.crm.conversion.dto.ConversionQueryRequest;
import com.btsheng.erp.business.crm.conversion.dto.ConversionRequest;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotation;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
import com.btsheng.erp.business.crm.conversion.service.AnnotationService;
import com.btsheng.erp.business.crm.conversion.service.ConversionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.8 · 工程转化 + 图纸标注 控制器
 *
 * 3 端点：
 * 1) POST /drawings/{id}/annotations    新增标注（AC-3.2.1）
 * 2) GET  /drawings/{id}/annotations    查询标注列表（AC-3.2.1）
 * 3) POST /drawings/{id}/convert        触发工程转化（AC-3.2.2）
 *
 * 复用 Story 1.5/1.6/1.7：
 * - DocNoGenerator.nextBomNo() BOM{yyyyMMdd}{seq:4}
 * - DrawingPdfExportService 5 段成本聚合
 * - DrawingEncryptionService AES-256-GCM
 */
@Tag(name = "E3-Drawing-Conversion", description = "工程转化 + 图纸标注（Story 1.8 · AC-3.2.1/2/3）")
@RestController
@RequestMapping("/drawings")
public class ConversionController {

    private final ConversionService conversionService;
    private final AnnotationService annotationService;

    @Autowired
    public ConversionController(ConversionService conversionService, AnnotationService annotationService) {
        this.conversionService = conversionService;
        this.annotationService = annotationService;
    }

    @Operation(summary = "新增图纸标注（AC-3.2.1 · P1 修补：必须挂载版本）")
    @ApiResponse(responseCode = "200", description = "标注新增成功")
    @PostMapping("/{id}/annotations")
    public Result<CrmDrawingAnnotation> addAnnotation(
            @Parameter(description = "图纸 ID") @PathVariable("id") Long drawingId,
            @RequestBody AnnotationRequest req,
            @Parameter(description = "操作人 ID（header）") @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return annotationService.addAnnotation(drawingId, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "查询图纸标注（按版本过滤 · AC-3.2.1）")
    @GetMapping("/{id}/annotations")
    public Result<?> listAnnotations(
            @Parameter(description = "图纸 ID") @PathVariable("id") Long drawingId,
            @Parameter(description = "版本号过滤（不传返回所有版本）") @RequestParam(value = "version", required = false) String version,
            @Parameter(description = "是否含图纸详情") @RequestParam(value = "withDrawing", required = false, defaultValue = "false") Boolean withDrawing) {
        if (Boolean.TRUE.equals(withDrawing)) {
            return annotationService.getDrawingWithAnnotations(drawingId, version);
        }
        if (version == null || version.isEmpty()) {
            return annotationService.listAllAnnotations(drawingId);
        }
        return annotationService.listAnnotationsByVersion(drawingId, version);
    }

    @Operation(summary = "触发工程转化（AC-3.2.2 · 40904 RELEASED 守卫）")
    @PostMapping("/{id}/convert")
    public Result<CrmDrawingConversion> convertDrawing(
            @Parameter(description = "图纸 ID") @PathVariable("id") Long drawingId,
            @RequestBody(required = false) ConversionRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        if (req == null) {
            req = new ConversionRequest();
        }
        return conversionService.convertDrawing(drawingId, req, operatorUserId == null ? 0L : operatorUserId);
    }
}
