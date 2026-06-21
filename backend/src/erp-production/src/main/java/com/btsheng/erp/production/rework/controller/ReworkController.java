package com.btsheng.erp.production.rework.controller;

import com.btsheng.erp.production.rework.dto.ReworkCreateRequest;
import com.btsheng.erp.production.rework.entity.CrmRework;
import com.btsheng.erp.production.rework.entity.CrmReworkHistory;
import com.btsheng.erp.production.rework.service.ReworkService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.23 · 委外返修闭环 Controller (FR-6-3)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /reworks                       创建返修单（AC-6.3.1）</li>
 *   <li>POST /reworks/{id}/finish           完成返修（AC-6.3.2）</li>
 *   <li>GET  /reworks/{outsourceId}/history 返修历史（AC-6.3.2）</li>
 *   <li>GET  /reworks/{outsourceId}/alert   返修次数预警（4 级别）</li>
 * </ul>
 */
@RestController
@RequestMapping("/reworks")
@Tag(name = "E6-Rework", description = "委外返修闭环（Story 1.23 FR-6-3）")
public class ReworkController {

    private final ReworkService service;

    @Autowired
    public ReworkController(ReworkService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建返修单（AC-6.3.1 · 返修次数 ≤ 3）")
    public Result<CrmRework> create(
            @RequestBody ReworkCreateRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.createRework(req.getOutsourceId(), req.getReason(), req.getCost(), userId);
    }

    @GetMapping
    @Operation(summary = "返修单列表")
    public Result<com.btsheng.erp.core.model.PageResponse<CrmRework>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.listReworks(keyword, pageNum, pageSize);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "返修单详情")
    public Result<CrmRework> getById(@PathVariable Long id) {
        return service.getReworkById(id);
    }

    @GetMapping("/alerts")
    @Operation(summary = "返修预警列表")
    public Result<com.btsheng.erp.core.model.PageResponse<Map<String, Object>>> listAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.listOpenAlerts(pageNum, pageSize);
    }

    @PostMapping("/alerts/{id}/ack")
    @Operation(summary = "确认处理返修预警")
    public Result<Void> ackAlert(@PathVariable Long id) {
        return service.ackAlert(id);
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "完成返修（AC-6.3.2）")
    public Result<CrmRework> finish(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.finishRework(id, userId);
    }

    @GetMapping("/{outsourceId}/history")
    @Operation(summary = "返修历史（AC-6.3.2 · 完整时间线）")
    public Result<List<CrmReworkHistory>> history(@PathVariable Long outsourceId) {
        return service.getReworkHistory(outsourceId);
    }

    @GetMapping("/{outsourceId}/alert")
    @Operation(summary = "返修次数预警（4 级别：INFO/WARN/CRITICAL/EXCEED）")
    public Result<Map<String, Object>> alert(@PathVariable Long outsourceId) {
        return service.getReworkAlert(outsourceId);
    }
}
