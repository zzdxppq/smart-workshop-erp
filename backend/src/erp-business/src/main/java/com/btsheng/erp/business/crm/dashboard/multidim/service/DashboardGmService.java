package com.btsheng.erp.business.crm.dashboard.multidim.service;

import com.btsheng.erp.business.crm.dashboard.outsource.service.OutsourceDashboardService;
import com.btsheng.erp.business.crm.dashboard.production.entity.CrmDashboardProduction;
import com.btsheng.erp.business.crm.dashboard.production.service.ProductionDashboardService;
import com.btsheng.erp.business.crm.report.sales.entity.CrmSalesReport;
import com.btsheng.erp.business.crm.report.sales.service.SalesReportService;
import com.btsheng.erp.business.finance.profit.entity.CrmProfitAnalysis;
import com.btsheng.erp.business.finance.profit.mapper.CrmProfitAnalysisMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** E11 · 总经理驾驶舱 / 工作台首页聚合 */
@Service
public class DashboardGmService {

    private final MultiDimDashboardService multiDimService;
    private final ProductionDashboardService productionDashboardService;
    private final OutsourceDashboardService outsourceDashboardService;
    private final CrmProfitAnalysisMapper profitMapper;
    private final SalesReportService salesReportService;

    @Autowired
    public DashboardGmService(MultiDimDashboardService multiDimService,
                              ProductionDashboardService productionDashboardService,
                              OutsourceDashboardService outsourceDashboardService,
                              CrmProfitAnalysisMapper profitMapper,
                              SalesReportService salesReportService) {
        this.multiDimService = multiDimService;
        this.productionDashboardService = productionDashboardService;
        this.outsourceDashboardService = outsourceDashboardService;
        this.profitMapper = profitMapper;
        this.salesReportService = salesReportService;
    }

    public Result<Map<String, Object>> getIndexSummary() {
        Map<String, Object> summary = new HashMap<>();
        Result<Map<String, Object>> prod = productionDashboardService.getKpiFlat();
        if (prod.isSuccess() && prod.getData() != null) {
            summary.put("productionCount", prod.getData().getOrDefault("activeWorkorders", 0));
            summary.put("pendingAlerts", prod.getData().getOrDefault("pendingAlerts", 0));
        }
        Result<Map<String, Object>> sales = multiDimService.getSalesDashboard(null, null, null);
        if (sales.isSuccess() && sales.getData() != null) {
            DashboardKpiMapper.enrichSales(sales.getData());
            summary.put("salesAmount", sales.getData().getOrDefault("monthAmount", 0));
            summary.put("monthOrders", sales.getData().getOrDefault("monthOrders", 0));
        }
        int todo = 0;
        List<CrmProfitAnalysis> profitAlerts = profitMapper.selectProfitAlerts(20);
        if (profitAlerts != null) todo += profitAlerts.size();
        Result<List<com.btsheng.erp.business.crm.dashboard.production.entity.CrmDashboardProduction>> alerts =
                productionDashboardService.getAlerts(20);
        if (alerts.isSuccess() && alerts.getData() != null) todo += alerts.getData().size();
        summary.put("todoCount", todo);
        summary.put("profitAlertCount", profitAlerts != null ? profitAlerts.size() : 0);
        return Result.ok(summary);
    }

    public Result<Map<String, Object>> getGmCockpit() {
        Map<String, Object> data = new HashMap<>();

        Result<Map<String, Object>> sales = multiDimService.getSalesDashboard(null, null, null);
        Result<Map<String, Object>> finance = multiDimService.getFinanceDashboard(null, null, null);
        Result<Map<String, Object>> production = multiDimService.getProductionDashboard(null, null, null);
        if (sales.isSuccess() && sales.getData() != null) DashboardKpiMapper.enrichSales(sales.getData());
        if (finance.isSuccess() && finance.getData() != null) DashboardKpiMapper.enrichFinance(finance.getData());
        if (production.isSuccess() && production.getData() != null) DashboardKpiMapper.enrichProduction(production.getData());

        data.put("sales", sales.getData());
        data.put("finance", finance.getData());
        data.put("production", production.getData());

        List<CrmProfitAnalysis> profitAlerts = profitMapper.selectProfitAlerts(10);
        List<Map<String, Object>> profitRows = new ArrayList<>();
        if (profitAlerts != null) {
            for (CrmProfitAnalysis p : profitAlerts) {
                Map<String, Object> row = new HashMap<>();
                row.put("orderNo", p.getOrderNo());
                row.put("customerName", p.getCustomerName());
                row.put("profitRate", p.getProfitRate());
                row.put("alertLevel", p.getAlertLevel());
                row.put("revenue", p.getRevenue());
                row.put("totalCost", p.getTotalCost());
                profitRows.add(row);
            }
        }
        data.put("profitAlerts", profitRows);

        Result<Map<String, Object>> outsource = outsourceDashboardService.getOverview(20);
        data.put("outsource", outsource.isSuccess() ? outsource.getData() : Map.of());

        Result<Map<String, Object>> ranking = salesReportService.getRanking(null, 10);
        data.put("salesRanking", toSalesRankingView(ranking));

        Result<Map<String, Object>> prodOverview = productionDashboardService.getOverview();
        data.put("productionOverview", prodOverview.isSuccess() ? prodOverview.getData() : Map.of());

        Result<List<CrmDashboardProduction>> prodAlerts = productionDashboardService.getAlerts(10);
        data.put("productionAlerts", toProductionAlertRows(prodAlerts));

        return Result.ok(data);
    }

    private static Map<String, Object> toSalesRankingView(Result<Map<String, Object>> ranking) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (ranking.isSuccess() && ranking.getData() != null) {
            Object listObj = ranking.getData().get("list");
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof CrmSalesReport r) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("rank", r.getRankNo());
                        row.put("salesman", r.getSalesUser());
                        row.put("customerName", r.getCustomerName());
                        row.put("amount", r.getAmount());
                        rows.add(row);
                    } else if (item instanceof Map<?, ?> m) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("rank", m.get("rankNo"));
                        row.put("salesman", m.get("salesUser"));
                        row.put("customerName", m.get("customerName"));
                        row.put("amount", m.get("amount"));
                        rows.add(row);
                    }
                }
            }
        }
        Map<String, Object> view = new HashMap<>();
        view.put("list", rows);
        return view;
    }

    private static List<Map<String, Object>> toProductionAlertRows(Result<List<CrmDashboardProduction>> prodAlerts) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (!prodAlerts.isSuccess() || prodAlerts.getData() == null) {
            return rows;
        }
        for (CrmDashboardProduction a : prodAlerts.getData()) {
            Map<String, Object> row = new HashMap<>();
            row.put("workorderNo", a.getWorkorderNo());
            row.put("alertMessage", a.getAlertMessage() != null ? a.getAlertMessage() : a.getProductName());
            row.put("alertType", a.getAlertType() != null ? a.getAlertType() : "WARN");
            rows.add(row);
        }
        return rows;
    }
}
