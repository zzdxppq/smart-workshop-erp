package com.btsheng.erp.business.finance.profit.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import com.btsheng.erp.business.finance.cost.entity.CrmCostSegment;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostAccountingMapper;
import com.btsheng.erp.business.finance.cost.mapper.CrmCostSegmentMapper;
import com.btsheng.erp.business.finance.profit.dto.AnalyzeProfitRequest;
import com.btsheng.erp.business.finance.profit.entity.CrmProfitAnalysis;
import com.btsheng.erp.business.finance.profit.mapper.CrmProfitAnalysisMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.web.SalesDataScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1.3.7 · Story 1.39 · 财务·利润分析 Service (FR-9-4)
 *
 * <p>4 业务方法：analyzeOrderProfit / getCustomerProfitRanking / getMonthlyTrend / exportProfitReport
 * <p>4 P1 修补�? * <ol>
 *   <li>利润 = 收入 - 5 段成�?/li>
 *   <li>利润�?= 利润 / 收入 * 100（支�?-100% ~ +∞，亏损订单 1.25 倍亏损）</li>
 *   <li>跨订�?1.6)+成本(1.37)+委外(1.18/1.22)+物料(1.13) 跨模块聚�?/li>
 *   <li>PDF 报告 1h 缓存（复�?1.5 模板�?/li>
 * </ol>
 */
@Service
public class ProfitAnalysisService {

    public static final String ALERT_NORMAL = "NORMAL";
    public static final String ALERT_WARNING = "WARNING";
    public static final String ALERT_CRITICAL = "CRITICAL";

    public static final BigDecimal WARNING_RATE = new BigDecimal("5.00");   // < 5% WARNING
            public static final BigDecimal CRITICAL_RATE = new BigDecimal("0.00"); // < 0% CRITICAL

    private final CrmProfitAnalysisMapper profitMapper;
    private final CrmCostAccountingMapper costMapper;
    private final CrmCostSegmentMapper segmentMapper;
    private final DocNoGenerator docNoGenerator;
    private final CrmOrderMapper orderMapper;
    private final ProfitThresholdResolver thresholdResolver;

    // P1 修补 4：PDF 1h 缓存
            private final ConcurrentHashMap<String, CachedReport> pdfCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60L * 60L * 1000L;  // 1 hour
            @Autowired
    public ProfitAnalysisService(CrmProfitAnalysisMapper profitMapper,
                                 CrmCostAccountingMapper costMapper,
                                 CrmCostSegmentMapper segmentMapper,
                                 DocNoGenerator docNoGenerator,
                                 CrmOrderMapper orderMapper,
                                 ProfitThresholdResolver thresholdResolver) {
        this.profitMapper = profitMapper;
        this.costMapper = costMapper;
        this.segmentMapper = segmentMapper;
        this.docNoGenerator = docNoGenerator;
        this.orderMapper = orderMapper;
        this.thresholdResolver = thresholdResolver;
    }

    /**
     * AC-9.4.1：分析订单利�?     * P1 修补 1：利�?= 收入 - 5 段成�?     * P1 修补 2：利润率 -100% ~ +�?     * P1 修补 3：跨订单+成本
     */
    @Transactional
    @AuditLog(module = "profit_analysis", action = "profit.analyze")
    public Result<Map<String, Object>> analyzeOrderProfit(AnalyzeProfitRequest req, Long operatorUserId) {
        if (req == null || req.getOrderId() == null) {
            return Result.fail(40001, "ORDER_ID_REQUIRED");
        }
        if (req.getRevenue() == null || req.getRevenue().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "REVENUE_INVALID");
        }
        if (req.getCustomerName() == null || req.getCustomerName().isEmpty()) {
            return Result.fail(40001, "CUSTOMER_NAME_REQUIRED");
        }
        if (req.getSettledDate() == null) {
            return Result.fail(40001, "SETTLED_DATE_REQUIRED");
        }
        if (profitMapper.selectByOrderId(req.getOrderId()) != null) {
            return Result.fail(40902, "PROFIT_DUPLICATE");
        }

        // P1 修补 3：跨成本�?.37 5 段自动归集）
            CrmCostAccounting cost = costMapper.selectByRef("ORDER", req.getOrderId());
        BigDecimal totalCost = BigDecimal.ZERO;
        Long costId = null;
        String costNo = null;
        if (cost != null) {
            costId = cost.getId();
            costNo = cost.getCostNo();
            List<CrmCostSegment> segs = segmentMapper.selectByCostId(cost.getId());
            if (segs != null) {
                for (CrmCostSegment s : segs) {
                    totalCost = totalCost.add(s.getAmount());
                }
            }
        }

        // P1 修补 1：利�?= 收入 - 5 段成�?
            BigDecimal profit = req.getRevenue().subtract(totalCost);
        // P1 修补 2：利润率（支�?-100% ~ +∞）
            BigDecimal profitRate = BigDecimal.ZERO;
        if (req.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
            profitRate = profit.multiply(new BigDecimal(100))
                    .divide(req.getRevenue(), 4, RoundingMode.HALF_UP);
        } else {
            // 收入 0 但成�?> 0 �?-∞（�?-1000 哨兵�?
            profitRate = totalCost.compareTo(BigDecimal.ZERO) > 0
                    ? new BigDecimal("-1000.0000")
                    : BigDecimal.ZERO;
        }

        String alertLevel = thresholdResolver.resolveAlertLevel(profitRate);

        CrmProfitAnalysis p = new CrmProfitAnalysis();
        p.setProfitNo(docNoGenerator.nextProfitAnalysisNo());
        p.setOrderId(req.getOrderId());
        p.setOrderNo(req.getOrderNo());
        p.setCustomerId(req.getCustomerId());
        p.setCustomerName(req.getCustomerName());
        p.setProductId(req.getProductId());
        p.setProductCode(req.getProductCode());
        p.setProductName(req.getProductName());
        p.setRevenue(req.getRevenue());
        p.setCostId(costId);
        p.setCostNo(costNo);
        p.setTotalCost(totalCost);
        p.setProfit(profit);
        p.setProfitRate(profitRate);
        p.setAlertLevel(alertLevel);
        p.setSettledDate(req.getSettledDate());
        p.setAnalysisMonth(req.getSettledDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        profitMapper.insert(p);

        Map<String, Object> data = new HashMap<>();
        data.put("profit", p);
        // 5 段明�?
            if (costId != null) {
            data.put("segments", segmentMapper.selectByCostId(costId));
        }
        return Result.ok(data);
    }

    /**
     * AC-9.4.1：客户利润排�?     */
    @AuditLog(module = "profit_analysis", action = "profit.ranking")
    public Result<Map<String, Object>> getCustomerProfitRanking() {
        Long ownerUserId = SalesDataScopeHelper.resolveListOwnerUserId();
        Long deptId = SalesDataScopeHelper.resolveListDeptId();
        List<Map<String, Object>> ranking = profitMapper.selectCustomerRankingScoped(ownerUserId, deptId);
        Map<String, Object> out = new HashMap<>();
        out.put("ranking", ranking == null ? new ArrayList<>() : ranking);
        out.put("count", ranking == null ? 0 : ranking.size());
        return Result.ok(out);
    }

    /** 前端看板读取 sys_param 利润率阈值 */
    public Result<Map<String, Object>> getProfitThresholds() {
        Map<String, Object> out = new HashMap<>();
        out.put("warningRate", thresholdResolver.getWarningRate());
        out.put("criticalRate", thresholdResolver.getCriticalRate());
        out.put("lossRate", thresholdResolver.getLossRate());
        return Result.ok(out);
    }

    /**
     * AC-9.4.1：月度利润趋�?     */
    @AuditLog(module = "profit_analysis", action = "profit.trend")
    public Result<Map<String, Object>> getMonthlyTrend() {
        Long ownerUserId = SalesDataScopeHelper.resolveListOwnerUserId();
        Long deptId = SalesDataScopeHelper.resolveListDeptId();
        List<Map<String, Object>> trend = profitMapper.selectMonthlyTrendScoped(ownerUserId, deptId);
        Map<String, Object> out = new HashMap<>();
        out.put("trend", trend == null ? new ArrayList<>() : trend);
        return Result.ok(out);
    }

    /**
     * AC-9.4.1 + P1 修补 4：导出利润报告（PDF 1h 缓存�?     */
    @AuditLog(module = "profit_analysis", action = "profit.export")
    public Result<Map<String, Object>> exportProfitReport(String month, Long operatorUserId) {
        if (month == null) month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String cacheKey = "profit-report-" + month;

        // P1 修补 4�?h 缓存
            CachedReport cached = pdfCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            Map<String, Object> data = new HashMap<>();
            data.put("month", month);
            data.put("report", cached.report);
            data.put("cached", true);
            data.put("cache_age_ms", System.currentTimeMillis() - cached.timestamp);
            return Result.ok(data);
        }

        List<CrmProfitAnalysis> list = profitMapper.selectByMonthScoped(
                month, SalesDataScopeHelper.resolveListOwnerUserId(), SalesDataScopeHelper.resolveListDeptId());
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        if (list != null) {
            for (CrmProfitAnalysis p : list) {
                if (p.getRevenue() != null) totalRevenue = totalRevenue.add(p.getRevenue());
                if (p.getTotalCost() != null) totalCost = totalCost.add(p.getTotalCost());
                if (p.getProfit() != null) totalProfit = totalProfit.add(p.getProfit());
            }
        }
        BigDecimal profitRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.multiply(new BigDecimal(100)).divide(totalRevenue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> report = new HashMap<>();
        report.put("month", month);
        report.put("total_revenue", totalRevenue);
        report.put("total_cost", totalCost);
        report.put("total_profit", totalProfit);
        report.put("profit_rate", profitRate);
        report.put("order_count", list == null ? 0 : list.size());
        report.put("details", list);
        report.put("generated_at", LocalDateTime.now());

        // 缓存
            pdfCache.put(cacheKey, new CachedReport(report, System.currentTimeMillis()));

        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        data.put("report", report);
        data.put("cached", false);
        return Result.ok(data);
    }

    /**
     * 合同/订单利润视图：优先读 crm_profit_analysis，否则按 Story 1.37 五段成本实时汇总�?     */
    public Result<Map<String, Object>> resolveOrderProfit(Long orderId) {
        if (orderId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "ORDER_ID_REQUIRED");
        }
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "ORDER_NOT_FOUND");
        }
        Result<Void> scope = SalesDataScopeHelper.assertOwnerDept(order.getOwnerUserId(), order.getDeptId());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }

        CrmProfitAnalysis existing = profitMapper.selectByOrderId(orderId);
        if (existing != null) {
            return Result.ok(buildProfitView(existing.getRevenue(), existing.getTotalCost(),
                    existing.getProfit(), existing.getProfitRate(), existing.getCostId()));
        }

        BigDecimal revenue = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        CrmCostAccounting cost = costMapper.selectByRef("ORDER", orderId);
        BigDecimal totalCost = sumSegmentCost(cost);
        BigDecimal profit = revenue.subtract(totalCost);
        BigDecimal marginRate = computeProfitRate(revenue, totalCost, profit);
        Long costId = cost != null ? cost.getId() : null;
        Map<String, Object> view = buildProfitView(revenue, totalCost, profit, marginRate, costId);
        view.put("source", "COST_ACCOUNTING");
        return Result.ok(view);
    }

    private Map<String, Object> buildProfitView(BigDecimal revenue, BigDecimal totalCost,
                                                 BigDecimal profit, BigDecimal marginRate, Long costId) {
        Map<String, Object> view = new HashMap<>();
        view.put("revenue", revenue);
        view.put("cost", totalCost);
        view.put("profit", profit);
        view.put("marginRate", marginRate != null ? marginRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        view.put("profitRate", marginRate != null ? marginRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        view.put("alertLevel", thresholdResolver.resolveAlertLevel(marginRate));
        if (costId != null) {
            view.put("segments", segmentMapper.selectByCostId(costId));
        } else {
            view.put("segments", List.of());
        }
        return view;
    }

    private BigDecimal sumSegmentCost(CrmCostAccounting cost) {
        if (cost == null) {
            return BigDecimal.ZERO;
        }
        List<CrmCostSegment> segs = segmentMapper.selectByCostId(cost.getId());
        if (segs == null || segs.isEmpty()) {
            return cost.getTotalCost() != null ? cost.getTotalCost() : BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CrmCostSegment s : segs) {
            if (s.getAmount() != null) {
                total = total.add(s.getAmount());
            }
        }
        return total;
    }

    private BigDecimal computeProfitRate(BigDecimal revenue, BigDecimal totalCost, BigDecimal profit) {
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            return profit.multiply(new BigDecimal("100")).divide(revenue, 4, RoundingMode.HALF_UP);
        }
        return totalCost.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("-1000.0000") : BigDecimal.ZERO;
    }

    /** 测试入口：清空缓�?*/
    public void clearCacheForTest() {
        pdfCache.clear();
    }

    private static class CachedReport {
        final Map<String, Object> report;
        final long timestamp;
        CachedReport(Map<String, Object> report, long timestamp) {
            this.report = report;
            this.timestamp = timestamp;
        }
    }
}
