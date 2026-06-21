package com.btsheng.erp.production.outsource.quality.controller;

import com.btsheng.erp.production.outsource.quality.dto.AddQualityDefectRequest;
import com.btsheng.erp.production.outsource.quality.dto.QualityCreateRequest;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQuality;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityDefect;
import com.btsheng.erp.production.outsource.quality.service.OutsourceQualityService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.27 · 委外工序质检 Controller (FR-6-7)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /outsource-quality              创建质检单（AC-6.7.1）</li>
 *   <li>POST /outsource-quality/{id}/defect  添加不良项（带严重度）</li>
 *   <li>POST /outsource-quality/{id}/pass    PASSED（AC-6.7.2）</li>
 *   <li>POST /outsource-quality/{id}/reject  FAILED（AC-6.7.3 · 触发返修）</li>
 *   <li>GET  /outsource-quality              列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/outsource-quality")
@Tag(name = "E6-Outsource-Quality", description = "委外工序质检（Story 1.27 FR-6-7）")
public class OutsourceQualityController {

    private final OutsourceQualityService service;

    @Autowired
    public OutsourceQualityController(OutsourceQualityService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建委外工序质检单（AC-6.7.1）")
    public Result<CrmOutsourceQuality> create(
            @RequestBody QualityCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createQuality(req, userId);
    }

    @PostMapping("/{id}/defect")
    @Operation(summary = "添加不良项（P1 修补 2 · 严重度分级 + 不良率 > 10% 告警）")
    public Result<CrmOutsourceQualityDefect> addDefect(
            @RequestBody AddQualityDefectRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.addDefect(req, userId);
    }

    @PostMapping("/{id}/pass")
    @Operation(summary = "标记 PASSED（AC-6.7.2）")
    public Result<CrmOutsourceQuality> pass(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.pass(id, userId);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "标记 FAILED（AC-6.7.3 · 触发返修）")
    public Result<Map<String, Object>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.reject(id, reason, userId);
    }

    @GetMapping
    @Operation(summary = "查询委外工序质检列表")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long outsourceId,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.listPaged(keyword, outsourceId, processName, result, pageNum, pageSize);
    }
}
