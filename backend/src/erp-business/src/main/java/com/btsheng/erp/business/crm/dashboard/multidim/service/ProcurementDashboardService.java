package com.btsheng.erp.business.crm.dashboard.multidim.service;

import com.btsheng.erp.core.model.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 采购驾驶舱 KPI（E11 扩展） */
@Service
public class ProcurementDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public ProcurementDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getProcurementDashboard() {
        int openRfq = queryInt("SELECT COUNT(*) FROM crm_rfq WHERE status IN ('OPEN','QUOTING','DRAFT')");
        int pendingPo = queryInt("SELECT COUNT(*) FROM crm_purchase_order WHERE status IN ('DRAFT','CONFIRMED','PENDING_SHIP')");
        int arriving = queryInt("SELECT COUNT(*) FROM crm_purchase_order WHERE status IN ('PARTIAL_ARRIVED','SHIPPING')");
        int pendingAlloc = 0;
        try {
            pendingAlloc = queryInt(
                    "SELECT COUNT(*) FROM cnc_production.outsub_allocation WHERE decision = 'OUTSOURCE'");
        } catch (Exception ignored) {
            pendingAlloc = queryInt(
                    "SELECT COUNT(*) FROM crm_rfq WHERE status = 'PENDING_OUTSOURCE'");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("openRfq", openRfq);
        data.put("pendingPo", pendingPo);
        data.put("arriving", arriving);
        data.put("pendingOutsource", pendingAlloc);
        data.put("metrics", buildMetrics(openRfq, pendingPo, arriving, pendingAlloc));
        return Result.ok(data);
    }

    private List<Map<String, Object>> buildMetrics(int openRfq, int pendingPo, int arriving, int pendingAlloc) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metric("开放询价", openRfq, "单"));
        metrics.add(metric("待执行 PO", pendingPo, "单"));
        metrics.add(metric("在途到货", arriving, "单"));
        metrics.add(metric("待委外工序", pendingAlloc, "项"));
        return metrics;
    }

    private Map<String, Object> metric(String name, Object value, String unit) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("value", value);
        m.put("unit", unit);
        return m;
    }

    private int queryInt(String sql) {
        try {
            Integer n = jdbcTemplate.queryForObject(sql, Integer.class);
            return n != null ? n : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
