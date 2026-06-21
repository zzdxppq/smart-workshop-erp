package com.btsheng.erp.business.crm.dashboard.outsource.controller;

import com.btsheng.erp.business.crm.dashboard.outsource.service.OutsourceDashboardService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "E11-Dashboard-Outsource", description = "报表·委外面板")
@RestController
@RequestMapping("/dashboard/outsource")
public class OutsourceDashboardController {

    private final OutsourceDashboardService service;

    @Autowired
    public OutsourceDashboardController(OutsourceDashboardService service) {
        this.service = service;
    }

    @Operation(summary = "委外概览（5 委外单 + 7 状态机 + 3 告警）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "50") int limit) {
        return service.getOverview(limit);
    }

    @Operation(summary = "委外质检面板")
    @GetMapping("/quality")
    public Result<Map<String, Object>> quality(@RequestParam(required = false) String vendor,
                                                @RequestParam(defaultValue = "50") int limit) {
        return service.getQuality(vendor, limit);
    }

    @Operation(summary = "委外成本面板")
    @GetMapping("/cost")
    public Result<Map<String, Object>> cost(@RequestParam(defaultValue = "50") int limit) {
        return service.getCost(limit);
    }

    /**
     * V1.3.8 Sprint 8 Story 8.6 · 委外成本占比（gm:summary outsourceCostRatio 精确统计）
     *
     * <p>返回 outsource_cost / total_cost 比值（0-1，4 位小数）
     * <p>权限：仅 GM + ADMIN（前端 gm-summary 已通过 @PreAuthorize 保护）
     */
    @GetMapping("/cost-ratio")
    @PreAuthorize("hasAnyRole('GM', 'ADMIN')")
    public Result<BigDecimal> costRatio() {
        return service.getCostRatio();
    }
}
