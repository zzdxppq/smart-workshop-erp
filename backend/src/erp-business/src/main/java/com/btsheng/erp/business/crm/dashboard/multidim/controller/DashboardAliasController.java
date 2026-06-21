package com.btsheng.erp.business.crm.dashboard.multidim.controller;

import com.btsheng.erp.business.crm.dashboard.multidim.service.DashboardDeliveryService;
import com.btsheng.erp.business.crm.dashboard.multidim.service.DashboardGmService;
import com.btsheng.erp.business.crm.dashboard.multidim.service.DashboardKpiMapper;
import com.btsheng.erp.business.crm.dashboard.multidim.service.MultiDimDashboardService;
import com.btsheng.erp.business.crm.dashboard.multidim.service.ProcurementDashboardService;
import com.btsheng.erp.business.crm.dashboard.outsource.entity.CrmOutsourceDashboard;
import com.btsheng.erp.business.crm.dashboard.outsource.service.OutsourceDashboardService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Web dashboard store 路径别名（/dashboard/sales → /dashboard/multidim/sales） */
@Tag(name = "E11-Dashboard-Alias", description = "看板路径别名")
@RestController
@RequestMapping("/dashboard")
public class DashboardAliasController {

    private final MultiDimDashboardService multiDimService;
    private final DashboardGmService gmService;
    private final OutsourceDashboardService outsourceDashboardService;
    private final DashboardDeliveryService deliveryService;
    private final ProcurementDashboardService procurementDashboardService;

    @Autowired
    public DashboardAliasController(MultiDimDashboardService multiDimService,
                                    DashboardGmService gmService,
                                    OutsourceDashboardService outsourceDashboardService,
                                    DashboardDeliveryService deliveryService,
                                    ProcurementDashboardService procurementDashboardService) {
        this.multiDimService = multiDimService;
        this.gmService = gmService;
        this.outsourceDashboardService = outsourceDashboardService;
        this.deliveryService = deliveryService;
        this.procurementDashboardService = procurementDashboardService;
    }

    @GetMapping("/sales")
    @Operation(summary = "销售驾驶舱（E11-S3）")
    public Result<Map<String, Object>> sales(@RequestParam(required = false) String dept,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String period) {
        Result<Map<String, Object>> r = multiDimService.getSalesDashboard(dept, category, period);
        if (r.isSuccess() && r.getData() != null) DashboardKpiMapper.enrichSales(r.getData());
        return r;
    }

    @GetMapping("/finance")
    @Operation(summary = "财务驾驶舱（E11-S3）")
    public Result<Map<String, Object>> finance(@RequestParam(required = false) String dept,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String period) {
        Result<Map<String, Object>> r = multiDimService.getFinanceDashboard(dept, category, period);
        if (r.isSuccess() && r.getData() != null) DashboardKpiMapper.enrichFinance(r.getData());
        return r;
    }

    @GetMapping("/quality")
    @Operation(summary = "品质驾驶舱")
    public Result<Map<String, Object>> quality(@RequestParam(required = false) String dept,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String period) {
        Result<Map<String, Object>> r = multiDimService.getQualityDashboard(dept, category, period);
        if (r.isSuccess() && r.getData() != null) DashboardKpiMapper.enrichQuality(r.getData());
        return r;
    }

    @GetMapping("/procurement")
    @Operation(summary = "采购驾驶舱 KPI")
    public Result<Map<String, Object>> procurement() {
        return procurementDashboardService.getProcurementDashboard();
    }

    @GetMapping("/delivery")
    @Operation(summary = "交付期检索（E11-S2 AC-11.2.3）")
    public Result<Map<String, Object>> delivery(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String customerKeyword,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String deliveryFrom,
            @RequestParam(required = false) String deliveryTo) {
        String kw = customerKeyword;
        if ((kw == null || kw.isBlank()) && customerId != null && !customerId.isBlank()) {
            kw = customerId;
        }
        return deliveryService.search(kw, status, deliveryFrom, deliveryTo);
    }

    @PostMapping("/delivery/template")
    @Operation(summary = "生成交付进度反馈文案")
    public Result<Map<String, Object>> deliveryTemplate(@RequestBody(required = false) Map<String, Object> body) {
        Long orderId = body != null && body.get("orderId") instanceof Number n ? n.longValue() : null;
        return deliveryService.buildFeedbackTemplate(
                orderId,
                body != null ? String.valueOf(body.getOrDefault("customerName", "")) : null,
                body != null ? String.valueOf(body.getOrDefault("orderNo", "")) : null,
                body != null ? String.valueOf(body.getOrDefault("currentStep", "")) : null,
                body != null && body.get("plannedDelivery") != null
                        ? LocalDate.parse(String.valueOf(body.get("plannedDelivery"))) : null);
    }

    @GetMapping("/outsource")
    @Operation(summary = "委外驾驶舱别名（扁平 KPI + overview）")
    public Result<Map<String, Object>> outsourceFlat(@RequestParam(defaultValue = "50") int limit) {
        Result<Map<String, Object>> raw = outsourceDashboardService.getOverview(limit);
        if (!raw.isSuccess() || raw.getData() == null) {
            return raw;
        }
        Map<String, Object> data = raw.getData();
        enrichOutsourceList(data);
        enrichOutsourceAlerts(data);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dist = (List<Map<String, Object>>) data.get("statusDistribution");
        int inProgress = 0;
        int delivered = 0;
        int delayed = 0;
        if (dist != null) {
            for (Map<String, Object> row : dist) {
                String status = String.valueOf(row.get("status"));
                int cnt = row.get("cnt") instanceof Number n ? n.intValue() : 0;
                if ("IN_PROGRESS".equals(status) || "PENDING".equals(status)) inProgress += cnt;
                else if ("COMPLETED".equals(status)) delivered += cnt;
                else if ("DELAYED".equals(status)) delayed += cnt;
            }
        }
        data.put("inProgress", inProgress);
        data.put("delivered", delivered);
        data.put("delayed", delayed);
        data.put("totalAmount", data.getOrDefault("totalCount", 0));
        return Result.ok(data);
    }

    @GetMapping("/outsource/export")
    @Operation(summary = "委外驾驶舱 Excel 导出（CSV）")
    public ResponseEntity<byte[]> exportOutsource(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String status) {
        Result<Map<String, Object>> raw = outsourceDashboardService.getOverview(limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw.isSuccess() && raw.getData() != null) {
            enrichOutsourceList(raw.getData());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) raw.getData().get("list");
            if (list != null) {
                for (Map<String, Object> row : list) {
                    if (vendor != null && !vendor.isBlank()
                            && !String.valueOf(row.get("vendorName")).contains(vendor)) continue;
                    if (status != null && !status.isBlank() && !status.equals(row.get("status"))) continue;
                    rows.add(row);
                }
            }
        }
        StringBuilder csv = new StringBuilder("\uFEFF委外单号,厂商,状态,工序,剩余天数,告警\n");
        for (Map<String, Object> r : rows) {
            csv.append(csvCell(r.get("outsourceNo"))).append(',')
                    .append(csvCell(r.get("vendorName"))).append(',')
                    .append(csvCell(r.get("status"))).append(',')
                    .append(csvCell(r.get("metricName"))).append(',')
                    .append(csvCell(r.get("remainingDays"))).append(',')
                    .append(csvCell(r.get("alertLevel"))).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=outsource-board.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/multidim/outsource/export")
    @Operation(summary = "多维度委外看板 Excel 导出（CSV）")
    public ResponseEntity<byte[]> exportMultidimOutsource() {
        return exportOutsource(50, null, null);
    }

    @GetMapping("/gm")
    @Operation(summary = "总经理驾驶舱（E11-S3 老板汇总）")
    public Result<Map<String, Object>> gmCockpit() {
        return gmService.getGmCockpit();
    }

    @SuppressWarnings("unchecked")
    private void enrichOutsourceList(Map<String, Object> data) {
        Object listObj = data.get("list");
        if (!(listObj instanceof List<?> list)) return;
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> row = new HashMap<>();
            if (item instanceof CrmOutsourceDashboard o) {
                row.put("outsourceNo", o.getOutsourceNo());
                row.put("vendorName", o.getVendorName());
                row.put("status", o.getStatus());
                row.put("metricName", o.getMetricName());
                row.put("metricValue", o.getMetricValue());
                row.put("qualityPassRate", o.getQualityPassRate());
                row.put("alertLevel", o.getAlertLevel());
                row.put("reworkCount", "HIGH".equals(o.getAlertLevel()) ? 2 : 0);
                row.put("remainingDays", computeRemainingDays(o));
            } else if (item instanceof Map<?, ?> m) {
                row.putAll((Map<String, Object>) m);
                if (!row.containsKey("remainingDays")) {
                    row.put("remainingDays", "DELAYED".equals(row.get("status")) ? -1 : 3);
                }
            }
            enriched.add(row);
        }
        data.put("list", enriched);
    }

    @SuppressWarnings("unchecked")
    private void enrichOutsourceAlerts(Map<String, Object> data) {
        Object alertsObj = data.get("alerts");
        if (!(alertsObj instanceof List<?> list)) return;
        List<Map<String, Object>> views = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> row = new HashMap<>();
            if (item instanceof CrmOutsourceDashboard o) {
                row.put("outsourceNo", o.getOutsourceNo());
                row.put("vendorName", o.getVendorName());
                row.put("status", o.getStatus());
                row.put("alertLevel", o.getAlertLevel());
                String msg = o.getMetricName();
                if (o.getMetricValue() != null) {
                    msg = (msg == null ? "告警" : msg) + "：" + o.getMetricValue();
                }
                row.put("alertMessage", msg != null ? msg : "委外异常");
            } else if (item instanceof Map<?, ?> m) {
                row.putAll((Map<String, Object>) m);
                if (!row.containsKey("alertMessage")) {
                    Object name = row.get("metricName");
                    Object val = row.get("metricValue");
                    row.put("alertMessage", val != null ? name + "：" + val : name);
                }
            }
            views.add(row);
        }
        data.put("alerts", views);
    }

    private int computeRemainingDays(CrmOutsourceDashboard o) {
        if ("DELAYED".equals(o.getStatus())) return -1;
        if ("HIGH".equals(o.getAlertLevel()) || "CRITICAL".equals(o.getAlertLevel())) return 0;
        if ("WARN".equals(o.getAlertLevel())) return 1;
        return 3;
    }

    private String csvCell(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        if (s.contains(",") || s.contains("\"")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
