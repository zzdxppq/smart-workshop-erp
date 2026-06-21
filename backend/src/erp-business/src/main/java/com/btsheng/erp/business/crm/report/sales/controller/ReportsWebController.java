package com.btsheng.erp.business.crm.report.sales.controller;

import com.btsheng.erp.business.crm.report.sales.entity.CrmSalesReport;
import com.btsheng.erp.business.crm.report.sales.service.SalesReportService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web 前端报表路径别名（/reports/*），与 OpenAPI /report/sales/* 并存。
 */
@Tag(name = "E11-Report-Web", description = "报表·Web 路径别名")
@RestController
@RequestMapping("/reports")
public class ReportsWebController {

    private final SalesReportService service;

    @Autowired
    public ReportsWebController(SalesReportService service) {
        this.service = service;
    }

    @Operation(summary = "销售龙虎榜（Web 路径）")
    @GetMapping("/sales-ranking")
    @PreAuthorize(PreAuthorizeRoles.REPORTS)
    public Result<Map<String, Object>> salesRanking(
            @RequestParam(required = false) String period,
            @RequestParam(required = false, name = "topN") Integer topN,
            @RequestParam(required = false) Integer limit) {
        int n = topN != null ? topN : (limit != null ? limit : 20);
        Result<Map<String, Object>> raw = service.getRanking(period, n);
        if (!raw.isSuccess() || raw.getData() == null) {
            return raw;
        }
        @SuppressWarnings("unchecked")
        List<CrmSalesReport> rows = (List<CrmSalesReport>) raw.getData().get("list");
        List<Map<String, Object>> view = new ArrayList<>();
        if (rows != null) {
            for (CrmSalesReport r : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("rank", r.getRankNo());
                m.put("customerName", r.getCustomerName());
                m.put("salesman", r.getSalesUser());
                m.put("amount", r.getAmount());
                m.put("orderCount", r.getOrderCount());
                m.put("growth", 0);
                view.add(m);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", view);
        data.put("period", period == null ? "" : period);
        data.put("total", view.size());
        return Result.ok(data);
    }

    @Operation(summary = "销售趋势（Web 路径）")
    @GetMapping("/sales-trend")
    @PreAuthorize(PreAuthorizeRoles.REPORTS)
    public Result<Map<String, Object>> salesTrend(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, name = "startDate") String startDate,
            @RequestParam(required = false, name = "endDate") String endDate) {
        String f = from != null ? from : toMonth(startDate);
        String t = to != null ? to : toMonth(endDate);
        if (f == null || t == null) {
            return Result.fail(Result.CODE_PARAM_FORMAT, "请提供 from/to 或 startDate/endDate");
        }
        Result<Map<String, Object>> raw = service.getTrend(f, t);
        if (!raw.isSuccess() || raw.getData() == null) {
            return raw;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw.getData().get("list");
        List<Map<String, Object>> view = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("period", row.get("period"));
                Object amt = row.get("totalAmount");
                m.put("revenue", amt);
                m.put("cost", 0);
                m.put("profit", amt);
                m.put("orderCount", row.get("totalOrders"));
                view.add(m);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", view);
        data.put("total", view.size());
        return Result.ok(data);
    }

    @Operation(summary = "客户分析（Web 路径）")
    @GetMapping("/customer-analysis")
    @PreAuthorize(PreAuthorizeRoles.REPORTS)
    public Result<Map<String, Object>> customerAnalysis(
            @RequestParam(required = false) String period,
            @RequestParam(required = false, name = "topN") Integer topN,
            @RequestParam(required = false) Integer limit) {
        int n = topN != null ? topN : (limit != null ? limit : 20);
        Result<Map<String, Object>> raw = service.getCustomerAnalysis(period, n);
        if (!raw.isSuccess() || raw.getData() == null) {
            return raw;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw.getData().get("list");
        List<Map<String, Object>> view = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Map<String, Object> m = new HashMap<>(row);
                Object total = row.get("totalAmount");
                Object cnt = row.get("totalOrders");
                int orders = cnt instanceof Number n1 ? n1.intValue() : 0;
                double amount = total instanceof Number n2 ? n2.doubleValue() : 0;
                m.put("customerName", row.get("customer"));
                m.put("avgAmount", orders > 0 ? amount / orders : 0);
                m.put("type", amount > 500000 ? "VIP" : "NORMAL");
                view.add(m);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", view);
        data.put("total", view.size());
        return Result.ok(data);
    }

    private static String toMonth(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return date.length() >= 7 ? date.substring(0, 7) : date;
    }
}
