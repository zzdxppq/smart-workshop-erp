package com.btsheng.erp.business.finance.materialcost.controller;

import com.btsheng.erp.business.finance.materialcost.entity.CrmMaterialCostAggregation;
import com.btsheng.erp.business.finance.materialcost.service.MaterialCostAggregationService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.40 · 财务·料号成本聚合 Controller (FR-9-5 V1.3.4 强化)
 *
 * <p>5 端点：
 * <ul>
 *   <li>POST /material-cost/aggregation        按物料 × 月份 × 厂商聚合（AC-9.5.1）</li>
 *   <li>GET  /material-cost/aggregation/{code} 按物料编码查 5 段</li>
 *   <li>GET  /material-cost/aggregation/trend  12 月趋势</li>
 *   <li>GET  /material-cost/aggregation/vendors 厂商对比</li>
 *   <li>GET  /material-cost/aggregation/export  导出（Excel/PDF · AC-9.5.2）</li>
 * </ul>
 */
@RestController
@RequestMapping("/material-cost/aggregation")
@Tag(name = "E9-Material-Cost-Aggregation", description = "财务·料号成本聚合（Story 1.40 FR-9-5 V1.3.4 强化）")
public class MaterialCostAggregationController {

    private final MaterialCostAggregationService service;

    @Autowired
    public MaterialCostAggregationController(MaterialCostAggregationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "按物料 × 月份 × 厂商聚合（AC-9.5.1）")
    public Result<Map<String, Object>> aggregate(
            @RequestBody CrmMaterialCostAggregation req,
            @RequestHeader(value = "X-User-Id", defaultValue = "703") Long userId) {
        return service.aggregateByMaterial(req, userId);
    }

    @GetMapping("/{materialCode}")
    @Operation(summary = "按物料编码查 5 段成本（跨月+跨厂商）")
    public Result<Map<String, Object>> getByMaterial(@PathVariable String materialCode) {
        return service.getMaterialCost(materialCode);
    }

    @GetMapping("/trend")
    @Operation(summary = "成本趋势 12 月窗口")
    public Result<Map<String, Object>> trend() {
        return service.getCostTrend();
    }

    @GetMapping("/vendors")
    @Operation(summary = "厂商对比（多 vendor）")
    public Result<Map<String, Object>> vendors(@RequestParam String materialCode) {
        return service.compareVendors(materialCode);
    }

    @GetMapping("/export")
    @Operation(summary = "导出（Excel/PDF · AC-9.5.2）")
    public Result<Map<String, Object>> export(
            @RequestParam String materialCode,
            @RequestParam(required = false, defaultValue = "excel") String format,
            @RequestHeader(value = "X-User-Id", defaultValue = "703") Long userId) {
        return service.exportMaterialCost(materialCode, format, userId);
    }
}
