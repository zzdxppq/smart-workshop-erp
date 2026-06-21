package com.btsheng.erp.business.crm.dashboard.materialprice.controller;

import com.btsheng.erp.business.crm.dashboard.materialprice.service.MaterialPriceDashboardService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "E11-Dashboard-Material-Price", description = "报表·料号价格面板")
@RestController
@RequestMapping("/dashboard/material-price")
public class MaterialPriceDashboardController {

    private final MaterialPriceDashboardService service;

    @Autowired
    public MaterialPriceDashboardController(MaterialPriceDashboardService service) {
        this.service = service;
    }

    @Operation(summary = "料号价格搜索")
    @GetMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String vendor,
                                               @RequestParam(defaultValue = "50") int limit) {
        return service.getPriceSearch(keyword, vendor, limit);
    }

    @Operation(summary = "价格趋势（12 月）")
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam String materialCode,
                                              @RequestParam String from,
                                              @RequestParam String to) {
        return service.getCostTrend(materialCode, from, to);
    }

    @Operation(summary = "厂商对比")
    @GetMapping("/vendor-compare")
    public Result<Map<String, Object>> vendorCompare(@RequestParam String materialCode,
                                                      @RequestParam String period) {
        return service.getVendorCompare(materialCode, period);
    }
}
