package com.btsheng.erp.business.crm.dashboard.outsource.service;

import com.btsheng.erp.business.crm.dashboard.outsource.entity.CrmOutsourceDashboard;
import com.btsheng.erp.business.crm.dashboard.outsource.mapper.CrmOutsourceDashboardMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.47 · 报表·委外面板 Service
 *
 * 3 方法：getOverview / getQuality / getCost
 * 3 P1 修补�? 状态机分布 / 委外质检不合格率 / �?1.22 + 1.27
 */
@Service
public class OutsourceDashboardService {

    public static final int MAX_OVERVIEW_LIMIT = 50;
    public static final int MAX_QUALITY_LIMIT = 50;
    public static final int MAX_COST_LIMIT = 50;

    /** 7 状态机合法状态（V1.3.4 修补�?*/
    public static final String[] SEVEN_STATES = {
        "PENDING", "IN_PROGRESS", "DELAYED", "INCOMING", "COMPLETED", "CANCELLED", "REWORK"
    };

    /** 委外质检不合格率上限（V1.3.4 修补 · 越界触发告警�?*/
    public static final BigDecimal DEFECT_RATE_ALERT = new BigDecimal("10.00");

    private final CrmOutsourceDashboardMapper dashboardMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public OutsourceDashboardService(CrmOutsourceDashboardMapper dashboardMapper,
                                      DocNoGenerator docNoGenerator) {
        this.dashboardMapper = dashboardMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-11.4.1 委外概览�? 委外�?+ 7 状态机分布 + 3 告警
     * P1 修补 1�? 状态机分布（V1.3.4�?     * P1 修补 3：跨 1.22 委外 + 1.27 质检
     */
    @AuditLog(module = "DASHBOARD_OUTSOURCE", action = "OVERVIEW")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getOverview(int limit) {
        if (limit < 1 || limit > MAX_OVERVIEW_LIMIT) limit = MAX_OVERVIEW_LIMIT;
        List<CrmOutsourceDashboard> overviewList = dashboardMapper.selectOverview(limit);
        List<Map<String, Object>> statusDist = dashboardMapper.selectStatusDistribution();
        List<CrmOutsourceDashboard> alerts = dashboardMapper.selectAlerts(null);
        Map<String, Object> data = new HashMap<>();
        data.put("dashboardNo", docNoGenerator.nextOutsourceDashboardNo());
        data.put("list", overviewList);
        data.put("statusDistribution", statusDist);
        data.put("alerts", alerts);
        data.put("totalCount", overviewList.size());
        data.put("alertCount", alerts.size());
        return Result.ok(data);
    }

    /**
     * AC-11.4.2 委外质检面板
     * P1 修补 2：委外质检不合格率（V1.3.4 · 1.27 复用�?     */
    @AuditLog(module = "DASHBOARD_OUTSOURCE", action = "QUALITY")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getQuality(String vendor, int limit) {
        if (limit < 1 || limit > MAX_QUALITY_LIMIT) limit = MAX_QUALITY_LIMIT;
        List<CrmOutsourceDashboard> rows = dashboardMapper.selectQuality(vendor, limit);
        List<Map<String, Object>> stats = dashboardMapper.selectQualityStats();
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("vendorStats", stats);
        data.put("alertThreshold", DEFECT_RATE_ALERT);
        return Result.ok(data);
    }

    /**
     * AC-11.4.3 委外成本面板
     * P1 修补 3：跨 1.22 委外 + 1.27 质检
     */
    @AuditLog(module = "DASHBOARD_OUTSOURCE", action = "COST")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getCost(int limit) {
        if (limit < 1 || limit > MAX_COST_LIMIT) limit = MAX_COST_LIMIT;
        List<CrmOutsourceDashboard> rows = dashboardMapper.selectCost(limit);
        List<Map<String, Object>> vendorCost = dashboardMapper.selectCostByVendor();
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("vendorCost", vendorCost);
        return Result.ok(data);
    }

    /**
     * V1.3.8 Sprint 8 Story 8.6 · 委外成本占比（gm:summary outsourceCostRatio 精确统计�?     *
     * <p>返回 outsource_cost / total_cost 比值（0-1�?     * <p>计算：SUM(委外订单总金�? / SUM(所有采购订单总金�?
     */
    @Transactional(readOnly = true)
    public Result<BigDecimal> getCostRatio() {
        BigDecimal outsourceTotal = dashboardMapper.selectOutsourceTotal();
        BigDecimal allPoTotal = dashboardMapper.selectAllPoTotal();
        if (allPoTotal == null || allPoTotal.compareTo(BigDecimal.ZERO) == 0) {
            return Result.ok(BigDecimal.ZERO);
        }
        BigDecimal ratio = outsourceTotal.divide(allPoTotal, 4, java.math.RoundingMode.HALF_UP);
        return Result.ok(ratio);
    }
}
