package com.btsheng.erp.business.crm.dashboard.production.controller;

import com.btsheng.erp.business.crm.dashboard.production.entity.CrmDashboardProduction;
import com.btsheng.erp.business.crm.dashboard.production.service.ProductionDashboardService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "E11-Dashboard-Production", description = "报表·生产工作台")
@RestController
@RequestMapping("/dashboard/production")
public class ProductionDashboardController {

    private final ProductionDashboardService service;

    @Autowired
    public ProductionDashboardController(ProductionDashboardService service) {
        this.service = service;
    }

    @Operation(summary = "生产总览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return service.getOverview();
    }

    @Operation(summary = "KPI 扁平数据（Web E11-S1 轮询）")
    @GetMapping(value = {"", "/"})
    public Result<Map<String, Object>> kpiFlat() {
        return service.getKpiFlat();
    }

    @Operation(summary = "工单列表")
    @GetMapping("/workorders")
    public Result<List<CrmDashboardProduction>> workorders(@RequestParam(defaultValue = "50") int limit) {
        return service.getWorkorders(limit);
    }

    @Operation(summary = "告警列表")
    @GetMapping("/alerts")
    public Result<List<CrmDashboardProduction>> alerts(@RequestParam(defaultValue = "20") int limit) {
        return service.getAlerts(limit);
    }
}
