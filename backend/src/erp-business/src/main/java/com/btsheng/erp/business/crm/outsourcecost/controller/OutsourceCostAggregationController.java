package com.btsheng.erp.business.crm.outsourcecost.controller;

import com.btsheng.erp.business.crm.outsourcecost.dto.AggregateCostRequest;
import com.btsheng.erp.business.crm.outsourcecost.entity.CrmOutsourceCostAggregation;
import com.btsheng.erp.business.crm.outsourcecost.service.OutsourceCostAggregationService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.26 · 委外成本归集 Controller (FR-6-6)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /outsource-cost/aggregate                  归集 5 段成本（AC-6.6.1/2）</li>
 *   <li>GET  /outsource-cost/{outsourceId}/segment      5 段成本聚合（AC-6.6.3）</li>
 *   <li>GET  /outsource-cost/{outsourceId}              按委外单查</li>
 *   <li>GET  /outsource-cost/export                    导出报表</li>
 * </ul>
 */
@RestController
@RequestMapping("/outsource-cost")
@Tag(name = "E6-Outsource-Cost", description = "委外成本归集（Story 1.26 FR-6-6）")
public class OutsourceCostAggregationController {

    private final OutsourceCostAggregationService service;

    @Autowired
    public OutsourceCostAggregationController(OutsourceCostAggregationService service) {
        this.service = service;
    }

    @PostMapping("/aggregate")
    @Operation(summary = "归集 5 段成本（AC-6.6.1 · 5 段自动累加）")
    public Result<CrmOutsourceCostAggregation> aggregate(
            @RequestBody AggregateCostRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.aggregateCost(req, userId);
    }

    @GetMapping("/{outsourceId}/segment")
    @Operation(summary = "5 段成本聚合（AC-6.6.3 · 与 1.10 闭环）")
    public Result<Map<String, Object>> segment(@PathVariable Long outsourceId) {
        return service.getCostBySegment(outsourceId);
    }

    @GetMapping("/{outsourceId}")
    @Operation(summary = "按委外单查询成本归集")
    public Result<List<CrmOutsourceCostAggregation>> byOutsource(@PathVariable Long outsourceId) {
        return service.exportCostReport(outsourceId, null);
    }

    @GetMapping("/export")
    @Operation(summary = "导出成本报表")
    public Result<List<CrmOutsourceCostAggregation>> export(
            @RequestParam(required = false) Long outsourceId,
            @RequestParam(required = false) String scope) {
        return service.exportCostReport(outsourceId, scope);
    }
}
