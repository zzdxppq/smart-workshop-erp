package com.btsheng.erp.business.crm.dashboard.materialprice.service;

import com.btsheng.erp.business.crm.dashboard.materialprice.entity.CrmMaterialPriceDashboard;
import com.btsheng.erp.business.crm.dashboard.materialprice.mapper.CrmMaterialPriceDashboardMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.48 · 报表·料号价格面板 Service
 *
 * 3 方法：getPriceSearch / getCostTrend / getVendorCompare
 * 3 P1 修补：物料编码唯一 / 价格趋势 12 月（V1.3.4�? �?1.33 + 1.40
 */
@Service
public class MaterialPriceDashboardService {

    public static final int MAX_SEARCH_LIMIT = 50;
    public static final int MAX_TREND_MONTHS = 12;

    private final CrmMaterialPriceDashboardMapper priceMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public MaterialPriceDashboardService(CrmMaterialPriceDashboardMapper priceMapper,
                                          DocNoGenerator docNoGenerator) {
        this.priceMapper = priceMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-11.5.1 料号价格搜索
     * P1 修补 1：物料编码唯一
     */
    @AuditLog(module = "DASHBOARD_MATERIAL_PRICE", action = "SEARCH")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getPriceSearch(String keyword, String vendor, int limit) {
        if (limit < 1 || limit > MAX_SEARCH_LIMIT) limit = MAX_SEARCH_LIMIT;
        List<CrmMaterialPriceDashboard> rows = priceMapper.searchPrice(keyword, vendor, limit);
        Map<String, Object> data = new HashMap<>();
        data.put("dashboardNo", docNoGenerator.nextOutsourceDashboardNo());
        data.put("list", rows);
        data.put("keyword", keyword == null ? "" : keyword);
        data.put("vendor", vendor == null ? "" : vendor);
        return Result.ok(data);
    }

    /**
     * AC-11.5.2 价格趋势�?2 月）
     * P1 修补 2：价格趋�?12 月（V1.3.4�?     */
    @AuditLog(module = "DASHBOARD_MATERIAL_PRICE", action = "TREND")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getCostTrend(String materialCode, String from, String to) {
        int months = calcMonthSpan(from, to);
        if (months > MAX_TREND_MONTHS) {
            return Result.fail(40003, "TREND_RANGE_EXCEED_12_MONTHS");
        }
        List<Map<String, Object>> rows = priceMapper.selectTrend(materialCode, from, to);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("materialCode", materialCode);
        data.put("from", from);
        data.put("to", to);
        return Result.ok(data);
    }

    /**
     * AC-11.5.3 厂商对比
     * P1 修补 3：跨 1.33 价格控制 + 1.40 料号成本聚合
     */
    @AuditLog(module = "DASHBOARD_MATERIAL_PRICE", action = "VENDOR_COMPARE")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getVendorCompare(String materialCode, String period) {
        List<Map<String, Object>> rows = priceMapper.selectVendorCompare(materialCode, period);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("materialCode", materialCode);
        data.put("period", period);
        return Result.ok(data);
    }

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
