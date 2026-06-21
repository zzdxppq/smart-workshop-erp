package com.btsheng.erp.business.finance.cost.controller;

import com.btsheng.erp.business.finance.cost.dto.AggregateCostRequest;
import com.btsheng.erp.business.finance.cost.service.CostAccountingService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.37 · 财务·成本核算 Controller (FR-9-2)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /cost-accounting/aggregate  5 段归集</li>
 *   <li>GET  /cost-accounting/segment    按段聚合</li>
 *   <li>GET  /cost-accounting/{refType}/{refId}  按引用查询</li>
 *   <li>POST /cost-accounting/list      列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/cost-accounting")
@Tag(name = "E9-Cost-Accounting", description = "财务·成本核算（Story 1.37 FR-9-2）")
public class CostAccountingController {

    private final CostAccountingService service;

    @Autowired
    public CostAccountingController(CostAccountingService service) {
        this.service = service;
    }

    @PostMapping("/aggregate")
    @Operation(summary = "5 段成本自动归集（AC-9.2.1）")
    public Result<Map<String, Object>> aggregate(
            @RequestBody AggregateCostRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "703") Long userId) {
        return service.aggregateCost(req, userId);
    }

    @GetMapping("/segment")
    @Operation(summary = "按段聚合（5 段总金额）")
    public Result<Map<String, Object>> bySegment() {
        return service.getCostBySegment();
    }

    @GetMapping("/{refType}/{refId}")
    @Operation(summary = "按引用查询（订单/工单/委外）")
    public Result<Map<String, Object>> byOrder(
            @PathVariable String refType,
            @PathVariable Long refId) {
        return service.getCostByOrder(refType, refId);
    }

    @PostMapping("/list")
    @Operation(summary = "列表（按 refType 过滤）")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String refType) {
        return service.listCosts(refType);
    }
}
