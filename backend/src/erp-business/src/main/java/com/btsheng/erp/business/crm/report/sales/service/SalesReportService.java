package com.btsheng.erp.business.crm.report.sales.service;

import com.btsheng.erp.business.crm.report.sales.entity.CrmSalesReport;
import com.btsheng.erp.business.crm.report.sales.mapper.CrmSalesReportMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.46 · 报表·销售排�?Service
 *
 * 3 方法：getRanking / getTrend / getCustomerAnalysis
 * 3 P1 修补：时间范�?�?12 �?/ 排行 Top 20 / �?1.6 订单 + 1.5 报价
 */
@Service
public class SalesReportService {

    private static final Logger log = LoggerFactory.getLogger(SalesReportService.class);

    public static final int MAX_RANKING_LIMIT = 20;
    public static final int MAX_TREND_MONTHS = 12;

    private final CrmSalesReportMapper reportMapper;

    @Autowired
    public SalesReportService(CrmSalesReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /**
     * AC-11.3.1 销售排行（按金额倒序�?     * P1 修补 2：Top 20 上限
     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getRanking(String period, int limit) {
        if (limit < 1 || limit > MAX_RANKING_LIMIT) limit = MAX_RANKING_LIMIT;
        try {
            List<CrmSalesReport> rows = reportMapper.selectRanking(period, limit);
            Map<String, Object> data = new HashMap<>();
            data.put("list", rows);
            data.put("period", period == null ? "" : period);
            data.put("topLimit", MAX_RANKING_LIMIT);
            return Result.ok(data);
        } catch (Exception ex) {
            log.warn("[SalesReport] getRanking fallback empty: {}", ex.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("list", List.of());
            data.put("period", period == null ? "" : period);
            data.put("topLimit", MAX_RANKING_LIMIT);
            return Result.ok(data);
        }
    }

    /**
     * AC-11.3.2 销售趋�?     * P1 修补 1：时间范�?�?12 �?     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getTrend(String fromPeriod, String toPeriod) {
        int months = calcMonthSpan(fromPeriod, toPeriod);
        if (months > MAX_TREND_MONTHS) {
            return Result.fail(40003, "TREND_RANGE_EXCEED_12_MONTHS");
        }
        try {
            List<Map<String, Object>> rows = reportMapper.selectTrend(fromPeriod, toPeriod);
            Map<String, Object> data = new HashMap<>();
            data.put("list", rows);
            data.put("from", fromPeriod);
            data.put("to", toPeriod);
            return Result.ok(data);
        } catch (Exception ex) {
            log.warn("[SalesReport] getTrend fallback empty: {}", ex.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("list", List.of());
            data.put("from", fromPeriod);
            data.put("to", toPeriod);
            return Result.ok(data);
        }
    }

    /**
     * AC-11.3.3 客户分析
     * P1 修补 3：跨 1.6 订单 + 1.5 报价
     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getCustomerAnalysis(String period, int limit) {
        if (limit < 1 || limit > MAX_RANKING_LIMIT) limit = MAX_RANKING_LIMIT;
        try {
            List<Map<String, Object>> rows = reportMapper.selectCustomerAnalysis(period, limit);
            Map<String, Object> data = new HashMap<>();
            data.put("list", rows);
            data.put("period", period == null ? "" : period);
            return Result.ok(data);
        } catch (Exception ex) {
            log.warn("[SalesReport] getCustomerAnalysis fallback empty: {}", ex.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("list", List.of());
            data.put("period", period == null ? "" : period);
            return Result.ok(data);
        }
    }

    /**
     * 简易月份跨度计算（yyyy-MM 格式�?     */
    private int calcMonthSpan(String from, String to) {
        if (from == null || to == null) return 0;
        try {
            String[] f = from.split("-");
            String[] t = to.split("-");
            int fy = Integer.parseInt(f[0]);
            int fm = Integer.parseInt(f[1]);
            int ty = Integer.parseInt(t[0]);
            int tm = Integer.parseInt(t[1]);
            return (ty - fy) * 12 + (tm - fm) + 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
