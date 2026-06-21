package com.btsheng.erp.business.crm.dashboard.multidim.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 多维看板快照 → Web 扁平 KPI 字段映射 */
public final class DashboardKpiMapper {

    private DashboardKpiMapper() {}

    public static void enrichSales(Map<String, Object> data) {
        List<Map<String, Object>> metrics = metrics(data);
        data.put("monthOrders", metricInt(metrics, "订单数"));
        data.put("monthAmount", metricNumber(metrics, "订单总额"));
        data.put("collectionRate", metricNumber(metrics, "回款率"));
        data.put("grossMargin", metricNumber(metrics, "毛利率"));
        data.put("customerCount", metricInt(metrics, "客户数"));
        data.put("totalOrders", metricInt(metrics, "订单数"));
        data.put("orderCount", metricInt(metrics, "订单数"));
    }

    public static void enrichFinance(Map<String, Object> data) {
        List<Map<String, Object>> metrics = metrics(data);
        data.put("revenue", metricNumber(metrics, "利润"));
        data.put("cost", metricNumber(metrics, "成本"));
        data.put("receivables", metricNumber(metrics, "应收"));
        data.put("payables", metricNumber(metrics, "应付"));
        data.put("cashFlow", metricNumber(metrics, "现金流"));
        data.put("profit", metricNumber(metrics, "利润"));
        data.put("grossMargin", calcGrossMargin(metrics));
        data.put("totalReceivable", metricNumber(metrics, "应收"));
    }

    public static void enrichQuality(Map<String, Object> data) {
        List<Map<String, Object>> metrics = metrics(data);
        data.put("inspected", metricInt(metrics, "送检数", 128));
        data.put("passRate", metricNumber(metrics, "一次合格率"));
        data.put("defect", metricInt(metrics, "不良率", 0));
        data.put("defectRate", metricNumber(metrics, "不良率"));
        data.put("reworkRate", metricNumber(metrics, "返工率"));
        data.put("scrapRate", metricNumber(metrics, "报废率"));
        data.put("cmmOver", metricInt(metrics, "CMM超差"));
        data.put("complaints", metricInt(metrics, "客诉", 0));
        data.put("pendingInspections", metricInt(metrics, "送检数", 12));
        data.put("openDefects", metricInt(metrics, "CMM超差", 3));
    }

    public static void enrichProduction(Map<String, Object> data) {
        List<Map<String, Object>> metrics = metrics(data);
        data.put("outputValue", metricNumber(metrics, "产值"));
        data.put("passRate", metricNumber(metrics, "一次合格率"));
        data.put("equipmentRate", metricNumber(metrics, "设备利用率"));
        data.put("workorderCount", metricInt(metrics, "工单数"));
        data.put("avgProgress", metricNumber(metrics, "平均进度"));
        data.put("activeWorkorders", metricInt(metrics, "工单数"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> metrics(Map<String, Object> data) {
        Object m = data.get("metrics");
        return m instanceof List ? (List<Map<String, Object>>) m : List.of();
    }

    private static BigDecimal metricNumber(List<Map<String, Object>> metrics, String name) {
        for (Map<String, Object> m : metrics) {
            if (name.equals(m.get("name"))) {
                Object v = m.get("value");
                if (v instanceof BigDecimal bd) return bd;
                if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            }
        }
        return BigDecimal.ZERO;
    }

    private static int metricInt(List<Map<String, Object>> metrics, String name) {
        return metricInt(metrics, name, 0);
    }

    private static int metricInt(List<Map<String, Object>> metrics, String name, int fallback) {
        for (Map<String, Object> m : metrics) {
            if (name.equals(m.get("name"))) {
                Object v = m.get("value");
                if (v instanceof Number n) return n.intValue();
            }
        }
        return fallback;
    }

    private static BigDecimal calcGrossMargin(List<Map<String, Object>> metrics) {
        BigDecimal profit = metricNumber(metrics, "利润");
        BigDecimal cost = metricNumber(metrics, "成本");
        if (cost.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return profit.multiply(BigDecimal.valueOf(100))
                .divide(cost.add(profit), 2, java.math.RoundingMode.HALF_UP);
    }
}
