package com.btsheng.erp.business.crm.dashboard.multidim.service;

import com.btsheng.erp.business.crm.dashboard.multidim.entity.CrmDashboardSnapshot;
import com.btsheng.erp.business.crm.dashboard.multidim.mapper.CrmDashboardSnapshotMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1.3.7 · Story 1.45 · 报表·多维度看�?Service
 *
 * 4 方法：getSalesDashboard / getProductionDashboard / getFinanceDashboard / getQualityDashboard
 * 6 维过滤：dimension / dept / category / period / metric_name / metric_value
 * 3 P1 修补�? 维过�?/ 图表数据格式统一 / 缓存 5min
 */
@Service
public class MultiDimDashboardService {

    public static final String DIM_SALES = "SALES";
    public static final String DIM_PRODUCTION = "PRODUCTION";
    public static final String DIM_FINANCE = "FINANCE";
    public static final String DIM_QUALITY = "QUALITY";

    public static final long CACHE_TTL_MS = 5 * 60 * 1000;  // 5min
            private final CrmDashboardSnapshotMapper snapshotMapper;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public MultiDimDashboardService(CrmDashboardSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getSalesDashboard(String dept, String category, String period) {
        return getDashboard(DIM_SALES, dept, category, period);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getProductionDashboard(String dept, String category, String period) {
        return getDashboard(DIM_PRODUCTION, dept, category, period);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getFinanceDashboard(String dept, String category, String period) {
        return getDashboard(DIM_FINANCE, dept, category, period);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getQualityDashboard(String dept, String category, String period) {
        return getDashboard(DIM_QUALITY, dept, category, period);
    }

    /**
     * P1 修补 1�? 维过滤（dimension 必填 + dept/category/period 可�?+ metric_name/value �?mapper 返回�?     * P1 修补 2：图表数据格式统一 {name, value, unit}
     * P1 修补 3：缓�?5min
     */
    private Result<Map<String, Object>> getDashboard(String dimension, String dept, String category, String period) {
        String key = dimension + "|" + (dept == null ? "" : dept) + "|" + (category == null ? "" : category) + "|" + (period == null ? "" : period);
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(key);
        if (cached != null && now - cached.timestamp < CACHE_TTL_MS) {
            return Result.ok(cached.data);
        }
        List<CrmDashboardSnapshot> rows = snapshotMapper.selectByDimension(dimension, dept, category, period);
        // P1 修补 2：统一图表格式
            List<Map<String, Object>> charts = new ArrayList<>();
        for (CrmDashboardSnapshot s : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", s.getMetricName());
            m.put("value", s.getMetricValue());
            m.put("unit", s.getMetricUnit());
            charts.add(m);
        }
        List<Map<String, Object>> trend = snapshotMapper.selectTrend(dimension);

        Map<String, Object> data = new HashMap<>();
        data.put("dimension", dimension);
        data.put("filters", Map.of("dept", dept == null ? "" : dept,
                                    "category", category == null ? "" : category,
                                    "period", period == null ? "" : period));
        data.put("metrics", charts);
        data.put("trend", trend);
        data.put("cacheTtlSec", 300);  // 5 min
            cache.put(key, new CacheEntry(data, now));
        return Result.ok(data);
    }

    private static class CacheEntry {
        final Map<String, Object> data;
        final long timestamp;
        CacheEntry(Map<String, Object> data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
