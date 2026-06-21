package com.btsheng.erp.business.crm.bom.controller;

import com.btsheng.erp.business.crm.bom.dto.*;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.service.BomService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.9 · BOM 多级维护 控制器
 *
 * 5 端点：
 * 1) POST  /boms                            创建 BOM（AC-3.3.1）
 * 2) GET   /boms/{id}                       查询详情
 * 3) PUT   /boms/{id}                       修改（仅 DRAFT）
 * 4) GET   /boms/{id}/tree                  多级树（AC-3.3.2）
 * 5) POST  /boms/{id}/convert-to-production 转生产（AC-3.3.4）
 * 6) POST  /boms/{id}/publish               发布（AC-3.3.5）
 * 7) GET   /boms                            列表
 */
@Tag(name = "E3-BOM", description = "BOM 多级维护（Story 1.9 · AC-3.3 · 5 段成本聚合）")
@RestController
@RequestMapping("/boms")
public class BomController {

    private final BomService bomService;

    @Autowired
    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @Operation(summary = "创建 BOM（AC-3.3.1）")
    @PostMapping
    public Result<CrmBom> createBom(
            @RequestBody BomCreateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return bomService.createBom(req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "查询 BOM 详情")
    @GetMapping("/{id}")
    public Result<CrmBom> getBom(@PathVariable("id") Long id) {
        return bomService.getBom(id);
    }

    @Operation(summary = "修改 BOM（仅 DRAFT 状态可改）")
    @PutMapping("/{id}")
    public Result<CrmBom> updateBom(
            @PathVariable("id") Long id,
            @RequestBody BomUpdateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        return bomService.updateBom(id, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "查询 BOM 多级树（AC-3.3.2 · 5 级递归上限）")
    @GetMapping("/{id}/tree")
    public Result<Map<String, Object>> getBomTree(@PathVariable("id") Long id) {
        return bomService.getBomTree(id);
    }

    @Operation(summary = "BOM 转生产（AC-3.3.4 · 生成 GD{yyyyMMdd}{seq:4} 工单）")
    @PostMapping("/{id}/convert-to-production")
    public Result<Map<String, Object>> convertToProduction(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ConvertToProductionRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        if (req == null) req = new ConvertToProductionRequest();
        return bomService.convertToProduction(id, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "BOM 发布（AC-3.3.5 · 发布后只读）")
    @PostMapping("/{id}/publish")
    public Result<CrmBom> releaseBom(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ReleaseBomRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId) {
        if (req == null) req = new ReleaseBomRequest();
        return bomService.releaseBom(id, req, operatorUserId == null ? 0L : operatorUserId);
    }

    @Operation(summary = "BOM 列表查询")
    @GetMapping
    public Result<Map<String, Object>> listBoms(BomQueryRequest query) {
        return bomService.listBoms(query);
    }

    @Operation(summary = "BOM 预览（按图纸 ID · 报价/订单行内折叠展示）")
    @GetMapping("/preview/by-drawing/{drawingId}")
    public Result<Map<String, Object>> getBomPreviewByDrawing(@PathVariable Long drawingId) {
        return bomService.getBomPreviewByDrawingId(drawingId);
    }

    @Operation(summary = "BOM 树保存（Web BomTree 组件）")
    @PostMapping("/save-tree")
    public Result<Map<String, Object>> saveTree(@RequestBody BomSaveTreeRequest req) {
        return bomService.saveTree(req);
    }
}
