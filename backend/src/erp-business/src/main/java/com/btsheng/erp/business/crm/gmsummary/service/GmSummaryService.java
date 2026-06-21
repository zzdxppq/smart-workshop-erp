package com.btsheng.erp.business.crm.gmsummary.service;

import com.btsheng.erp.business.crm.gmsummary.dto.GmSummaryDTO;
import com.btsheng.erp.business.crm.gmsummary.mapper.GmSummaryMapper;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 · Story 4.3 · 总经理汇总报表 Service
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #getSummary} AC-4.3.1 6 项指标 + trend_chart</li>
 * </ul>
 *
 * <p>权限：仅 GM + PROCUREMENT_MANAGER 可见（@PreAuthorize 在 Controller）
 * <p>Redis 缓存：Sprint 7 集成阶段实装 @Cacheable(value="gm:summary", key="#period", ttl=300)
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Service
public class GmSummaryService {

    private static final Logger log = LoggerFactory.getLogger(GmSummaryService.class);

    public static final String PERIOD_LAST_7D = "LAST_7D";
    public static final String PERIOD_LAST_30D = "LAST_30D";
    public static final String PERIOD_LAST_90D = "LAST_90D";
    public static final String PERIOD_CUSTOM = "CUSTOM";

    private final GmSummaryMapper mapper;

    @Autowired
    public GmSummaryService(GmSummaryMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * AC-4.3.1：汇总聚合（6 项指标 + trend）
     * <p>V1.3.8 Sprint 7 集成 A：Redis 5min 缓存
     * <ul>
     *   <li>value="gm:summary" 全局缓存命名空间</li>
     *   <li>key=#period 单周期维度（LAST_7D/30D/90D/CUSTOM）</li>
     *   <li>CUSTOM 模式不走缓存（startDate/endDate 变化频繁）</li>
     * </ul>
     * <p>缓存失效：@CacheEvict allEntries=true（4.1 PO 创建 / 4.2 审批完成触发）
     */
    @Cacheable(value = "gm:summary", key = "#period", unless = "#result == null || !#result.isSuccess() || #period == 'CUSTOM'")
    public Result<GmSummaryDTO> getSummary(String period, LocalDate startDate, LocalDate endDate) {
        // 1. 周期解析
            LocalDate[] range = resolvePeriod(period, startDate, endDate);
        if (range == null) {
            return Result.fail(Result.CODE_PARAM_FORMAT, "invalid period: " + period);
        }
        LocalDate start = range[0];
        LocalDate end = range[1];
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();

        Map<String, Object> metrics = safeAggregateMetrics(startDt, endDt);
        Integer pmWorkload = safeProcurementManagerWorkload(startDt, endDt);
        Double outsourceRatio = safeOutsourceCostRatio();

        GmSummaryDTO dto = new GmSummaryDTO();
        dto.setPeriod(period);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setNoOrderPoCount(toInt(metrics.get("no_order_count")));
        dto.setNoOrderPoAmount(toBigDecimal(metrics.get("no_order_amount")));
        dto.setUrgentReplenishCount(toInt(metrics.get("urgent_count")));
        dto.setAmountThresholdPassedRate(toBigDecimal(metrics.get("pass_rate")));
        dto.setProcurementManagerWorkload(pmWorkload != null ? pmWorkload : 0);
        dto.setOutsourceCostRatio(outsourceRatio != null ? BigDecimal.valueOf(outsourceRatio) : BigDecimal.ZERO);

        // 3. trend_chart：真实 SQL 聚合（表缺失时返回空趋势）
        List<GmSummaryDTO.TrendPoint> trend = new ArrayList<>();
        try {
            List<Map<String, Object>> trendRows = mapper.trendChart(startDt, endDt);
            for (Map<String, Object> row : trendRows) {
                GmSummaryDTO.TrendPoint p = new GmSummaryDTO.TrendPoint();
                Object dateObj = row.get("trend_date");
                if (dateObj instanceof java.sql.Date sqlDate) {
                    p.setDate(sqlDate.toLocalDate());
                } else if (dateObj instanceof LocalDate ld) {
                    p.setDate(ld);
                }
                p.setNoOrderCount(toInt(row.get("no_order_count")));
                p.setAmount(toBigDecimal(row.get("amount")));
                trend.add(p);
            }
        } catch (Exception ex) {
            log.warn("[GmSummaryService] trendChart fallback empty: {}", ex.getMessage());
        }
        dto.setTrendChart(trend);

        log.info("[GmSummaryService] getSummary ok: period={} range=[{},{}] noOrder={}",
                period, start, end, dto.getNoOrderPoCount());
        return Result.ok(dto);
    }

    /**
     * V1.3.8 Sprint 7 集成 A：缓存失效（Story 4.3 §2.2）
     * <p>4.1 NoOrderPurchaseService.createNoOrderPurchase AFTER_COMMIT 调用
     * <p>4.2 ProcurementApprovalRouter 审批完成 AFTER_COMMIT 调用
     * <p>allEntries=true 清空所有 period 缓存（LAST_7D/30D/90D）
     */
    @CacheEvict(value = "gm:summary", allEntries = true)
    public void evictCache() {
        log.info("[GmSummaryService] evictCache: allEntries cleared");
    }

    private Map<String, Object> safeAggregateMetrics(LocalDateTime start, LocalDateTime end) {
        try {
            Map<String, Object> m = mapper.aggregateMetrics(start, end);
            return m != null ? m : Map.of();
        } catch (Exception ex) {
            log.warn("[GmSummaryService] aggregateMetrics fallback zero: {}", ex.getMessage());
            return Map.of();
        }
    }

    private Integer safeProcurementManagerWorkload(LocalDateTime start, LocalDateTime end) {
        try {
            return mapper.countProcurementManagerWorkload(start, end);
        } catch (Exception ex) {
            log.warn("[GmSummaryService] pmWorkload fallback zero: {}", ex.getMessage());
            return 0;
        }
    }

    private Double safeOutsourceCostRatio() {
        try {
            return mapper.selectOutsourceCostRatio();
        } catch (Exception ex) {
            log.warn("[GmSummaryService] outsourceRatio fallback zero: {}", ex.getMessage());
            return 0.0;
        }
    }

    /**
     * 周期解析
     */
    private LocalDate[] resolvePeriod(String period, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        return switch (period == null ? "" : period) {
            case PERIOD_LAST_7D -> new LocalDate[]{today.minusDays(7), today};
            case PERIOD_LAST_30D -> new LocalDate[]{today.minusDays(30), today};
            case PERIOD_LAST_90D -> new LocalDate[]{today.minusDays(90), today};
            case PERIOD_CUSTOM -> {
                if (startDate == null || endDate == null) yield null;
                yield new LocalDate[]{startDate, endDate};
            }
            default -> null;
        };
    }

    /** MySQL COUNT/SUM 返回类型兜底 */
    private Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return 0;
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}