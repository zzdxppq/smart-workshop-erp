package com.btsheng.erp.business.crm.report.sales.controller;

import com.btsheng.erp.business.crm.report.sales.service.SalesReportService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "E11-Report-Sales", description = "报表·销售排行")
@RestController
@RequestMapping("/report/sales")
public class SalesReportController {

    private final SalesReportService service;

    @Autowired
    public SalesReportController(SalesReportService service) {
        this.service = service;
    }

    @Operation(summary = "销售排行")
    @GetMapping("/ranking")
    public Result<Map<String, Object>> ranking(@RequestParam(required = false) String period,
                                                @RequestParam(defaultValue = "20") int limit) {
        return service.getRanking(period, limit);
    }

    @Operation(summary = "销售趋势")
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam String from,
                                              @RequestParam String to) {
        return service.getTrend(from, to);
    }

    @Operation(summary = "客户分析")
    @GetMapping("/customer-analysis")
    public Result<Map<String, Object>> customer(@RequestParam(required = false) String period,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return service.getCustomerAnalysis(period, limit);
    }
}
