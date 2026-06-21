package com.btsheng.erp.business.crm.dashboard.production.service;

import com.btsheng.erp.business.crm.dashboard.production.entity.CrmDashboardProduction;
import com.btsheng.erp.business.crm.dashboard.production.mapper.CrmDashboardProductionMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.44 · 报表·生产工作�?Service
 *
 * 3 方法：getOverview / getWorkorders / getAlerts
 * 3 P1 修补：工�?4 状态分�?/ 告警 3 级别 / 实时刷新 �?5s
 */
@Service
public class ProductionDashboardService {

    private final CrmDashboardProductionMapper dashboardMapper;

    @Autowired
    public ProductionDashboardService(CrmDashboardProductionMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    /**
     * AC-11.1.1 总览：工单分�?+ 进度 + 告警级别分布
     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = dashboardMapper.selectOverview();
        List<Map<String, Object>> woDist = dashboardMapper.selectWorkorderStatusDistribution();
        List<Map<String, Object>> alertDist = dashboardMapper.selectAlertLevelDistribution();
        Map<String, Object> data = new HashMap<>();
        data.put("overview", overview);
        data.put("workorderStatus", woDist);
        data.put("alertLevel", alertDist);
        data.put("refreshInterval", 5);  // P1 修补 3：实时刷�?�?5s
            return Result.ok(data);
    }

    /**
     * AC-11.1.2 工单列表
     */
    @Transactional(readOnly = true)
    public Result<List<CrmDashboardProduction>> getWorkorders(int limit) {
        if (limit < 1 || limit > 200) limit = 50;
        return Result.ok(dashboardMapper.selectWorkorders(limit));
    }

    /**
     * AC-11.1.3 告警列表
     */
    @Transactional(readOnly = true)
    public Result<List<CrmDashboardProduction>> getAlerts(int limit) {
        if (limit < 1 || limit > 200) limit = 20;
        return Result.ok(dashboardMapper.selectAlerts(limit));
    }

    /**
     * P1 修补 1�? 状态分布计�?     */
    public int countByStatus(List<Map<String, Object>> dist, String status) {
        if (dist == null) return 0;
        for (Map<String, Object> m : dist) {
            if (status.equals(m.get("status"))) {
                Object cnt = m.get("cnt");
                return cnt == null ? 0 : Integer.parseInt(cnt.toString());
            }
        }
        return 0;
    }

    /**
     * P1 修补 2�? 级别判定（INFO/WARN/CRITICAL�?     */
    public boolean isValidAlertLevel(String level) {
        return "INFO".equals(level) || "WARN".equals(level) || "CRITICAL".equals(level);
    }

    /** Web E11-S1 KPI 扁平结构（GET /dashboard/production�?*/
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getKpiFlat() {
        Map<String, Object> overview = dashboardMapper.selectOverview();
        List<Map<String, Object>> woDist = dashboardMapper.selectWorkorderStatusDistribution();
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("activeWorkorders", countByStatus(woDist, "IN_PROGRESS"));
        kpi.put("todayFinished", overview != null && overview.get("todayFinished") != null
                ? overview.get("todayFinished") : 0);
        kpi.put("equipmentRate", overview != null && overview.get("equipmentRate") != null
                ? overview.get("equipmentRate") : 0);
        kpi.put("pendingAlerts", overview != null && overview.get("pendingAlerts") != null
                ? overview.get("pendingAlerts") : 0);
        kpi.put("refreshInterval", 5);
        return Result.ok(kpi);
    }

    /** Spec B.2 · dashboard:kanban 轮询 */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getKanbanFeed(int limit) {
        List<CrmDashboardProduction> rows = dashboardMapper.selectWorkorders(limit < 1 ? 50 : limit);
        List<Map<String, Object>> items = new ArrayList<>();
        for (CrmDashboardProduction row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getId());
            item.put("workorderNo", row.getWorkorderNo());
            item.put("materialCode", row.getProductName());
            item.put("progress", row.getProgress() != null ? row.getProgress().intValue() : 0);
            String status = row.getWorkorderStatus();
            if ("SCHEDULED".equals(status) || "PENDING".equals(status)) {
                item.put("status", "PENDING");
            } else if ("IN_PROGRESS".equals(status)) {
                item.put("status", "IN_PROGRESS");
            } else if ("INSPECT".equals(status) || "QC".equals(status)) {
                item.put("status", "INSPECT");
            } else if (row.getAlertType() != null) {
                item.put("status", "ALERT");
            } else {
                item.put("status", status);
            }
            items.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return Result.ok(data);
    }

    /** Spec B.2 · dashboard:events 轮询 */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getEventsFeed(int limit) {
        List<CrmDashboardProduction> alerts = dashboardMapper.selectAlerts(limit < 1 ? 20 : limit);
        List<Map<String, Object>> events = new ArrayList<>();
        for (CrmDashboardProduction a : alerts) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("time", a.getSnapshotAt() != null ? a.getSnapshotAt().toString() : "");
            ev.put("type", a.getAlertType() != null ? a.getAlertType() : "INFO");
            ev.put("message", a.getAlertMessage() != null ? a.getAlertMessage() : a.getWorkorderNo());
            events.add(ev);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("events", events);
        return Result.ok(data);
    }
}
