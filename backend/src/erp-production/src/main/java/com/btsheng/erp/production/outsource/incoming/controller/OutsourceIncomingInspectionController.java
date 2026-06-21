package com.btsheng.erp.production.outsource.incoming.controller;

import com.btsheng.erp.production.outsource.incoming.dto.AddDefectRequest;
import com.btsheng.erp.production.outsource.incoming.dto.IncomingInspectionRequest;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingDefect;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingInspection;
import com.btsheng.erp.production.outsource.incoming.service.OutsourceIncomingInspectionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.25 · 委外来料质检 Controller (FR-6-5)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /outsource-incoming              创建来料质检单（AC-6.5.1）</li>
 *   <li>POST /outsource-incoming/{id}/pass    标记 PASSED（AC-6.5.2）</li>
 *   <li>POST /outsource-incoming/{id}/reject  标记 FAILED（AC-6.5.3 · 触发返修）</li>
 *   <li>GET  /outsource-incoming              查询列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/outsource-incoming")
@Tag(name = "E6-Outsource-Incoming-Inspection", description = "委外来料质检（Story 1.25 FR-6-5）")
public class OutsourceIncomingInspectionController {

    private final OutsourceIncomingInspectionService service;

    @Autowired
    public OutsourceIncomingInspectionController(OutsourceIncomingInspectionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建来料质检单（AC-6.5.1）")
    public Result<CrmOutsourceIncomingInspection> create(
            @RequestBody IncomingInspectionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createInspection(req, userId);
    }

    @PostMapping("/{id}/defect")
    @Operation(summary = "添加不良项（P1 修补 3 · 严重度分级）")
    public Result<CrmOutsourceIncomingDefect> addDefect(
            @RequestBody AddDefectRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.addDefect(req, userId);
    }

    @PostMapping("/{id}/pass")
    @Operation(summary = "标记 PASSED（AC-6.5.2）")
    public Result<CrmOutsourceIncomingInspection> pass(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.pass(id, userId);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "标记 FAILED（AC-6.5.3 · 触发返修）")
    public Result<Map<String, Object>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.reject(id, reason, userId);
    }

    @PostMapping("/{id}/conditional")
    @Operation(summary = "标记 CONDITIONAL（AC-6.5.2 · 不良率 ≤ 10%）")
    public Result<CrmOutsourceIncomingInspection> conditional(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.conditional(id, userId);
    }

    @GetMapping
    @Operation(summary = "查询来料质检列表")
    public Result<List<CrmOutsourceIncomingInspection>> list(
            @RequestParam(required = false) Long outsourceId,
            @RequestParam(required = false) String result) {
        return service.list(outsourceId, result);
    }
}
