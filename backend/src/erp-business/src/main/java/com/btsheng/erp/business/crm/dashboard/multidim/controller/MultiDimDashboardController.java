package com.btsheng.erp.business.crm.dashboard.multidim.controller;

import com.btsheng.erp.business.crm.dashboard.multidim.service.MultiDimDashboardService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "E11-Dashboard-Multidim", description = "报表·多维度看板")
@RestController
@RequestMapping("/dashboard/multidim")
public class MultiDimDashboardController {

    private final MultiDimDashboardService service;

    @Autowired
    public MultiDimDashboardController(MultiDimDashboardService service) {
        this.service = service;
    }

    @Operation(summary = "销售看板")
    @GetMapping("/sales")
    public Result<Map<String, Object>> sales(@RequestParam(required = false) String dept,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String period) {
        return service.getSalesDashboard(dept, category, period);
    }

    @Operation(summary = "生产看板")
    @GetMapping("/production")
    public Result<Map<String, Object>> production(@RequestParam(required = false) String dept,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String period) {
        return service.getProductionDashboard(dept, category, period);
    }

    @Operation(summary = "财务看板")
    @GetMapping("/finance")
    public Result<Map<String, Object>> finance(@RequestParam(required = false) String dept,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String period) {
        return service.getFinanceDashboard(dept, category, period);
    }

    @Operation(summary = "质量看板")
    @GetMapping("/quality")
    public Result<Map<String, Object>> quality(@RequestParam(required = false) String dept,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String period) {
        return service.getQualityDashboard(dept, category, period);
    }
}
