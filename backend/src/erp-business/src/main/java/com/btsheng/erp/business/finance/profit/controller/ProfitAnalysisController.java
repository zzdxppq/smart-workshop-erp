package com.btsheng.erp.business.finance.profit.controller;

import com.btsheng.erp.business.finance.profit.dto.AnalyzeProfitRequest;
import com.btsheng.erp.business.finance.profit.service.ProfitAnalysisService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V1.3.7 · Story 1.39 · 财务·利润分析 Controller (FR-9-4)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /profit-analysis/analyze         分析订单利润（AC-9.4.1）</li>
 *   <li>GET  /profit-analysis/ranking         客户利润排行</li>
 *   <li>GET  /profit-analysis/trend            月度利润趋势</li>
 *   <li>GET  /profit-analysis/export           导出利润报告（PDF 1h 缓存）</li>
 * </ul>
 */
@RestController
@RequestMapping("/profit-analysis")
@Tag(name = "E9-Profit-Analysis", description = "财务·利润分析（Story 1.39 FR-9-4）")
public class ProfitAnalysisController {

    private final ProfitAnalysisService service;

    @Autowired
    public ProfitAnalysisController(ProfitAnalysisService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    @Operation(summary = "分析订单利润（AC-9.4.1）")
    public Result<Map<String, Object>> analyze(
            @RequestBody AnalyzeProfitRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "703") Long userId) {
        return service.analyzeOrderProfit(req, userId);
    }

    @GetMapping("/ranking")
    @Operation(summary = "客户利润排行")
    public Result<Map<String, Object>> ranking() {
        return service.getCustomerProfitRanking();
    }

    @GetMapping("/thresholds")
    @Operation(summary = "利润率预警阈值（sys_param）")
    public Result<Map<String, Object>> thresholds() {
        return service.getProfitThresholds();
    }

    @GetMapping("/trend")
    @Operation(summary = "月度利润趋势")
    public Result<Map<String, Object>> trend() {
        return service.getMonthlyTrend();
    }

    @GetMapping("/export")
    @Operation(summary = "导出利润报告（PDF 1h 缓存）")
    public Result<Map<String, Object>> export(
            @RequestParam(required = false) String month,
            @RequestHeader(value = "X-User-Id", defaultValue = "703") Long userId) {
        return service.exportProfitReport(month, userId);
    }
}
