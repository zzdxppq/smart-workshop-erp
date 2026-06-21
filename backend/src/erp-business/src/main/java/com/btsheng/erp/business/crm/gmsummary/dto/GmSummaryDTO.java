package com.btsheng.erp.business.crm.gmsummary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * V1.3.8 · Story 4.3 · 总经理汇总报表 DTO
 *
 * <p>6 项指标 + trend_chart：
 * <ul>
 *   <li>无订单采购 PO 数 / 金额（Story 4.1 source_type=NO_ORDER）</li>
 *   <li>紧急补料频次（Story 4.1 purchase_reason=URGENT_REPLENISH）</li>
 *   <li>金额阈值审批通过率（Story 4.2 amount > 10000）</li>
 *   <li>PROCUREMENT_MANAGER 工作量（Story 4.2 approver_role）</li>
 *   <li>委外成本占比（V1.3.7 1.45 看板）</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "总经理汇总报表（Story 4.3 AC-4.3.1）")
public class GmSummaryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "统计周期 LAST_7D / LAST_30D / LAST_90D / CUSTOM")
    private String period;

    private LocalDate startDate;
    private LocalDate endDate;

    /** 无订单采购 PO 数 */
    private Integer noOrderPoCount;

    /** 无订单采购总金额 */
    private BigDecimal noOrderPoAmount;

    /** 紧急补料频次 */
    private Integer urgentReplenishCount;

    /** 金额阈值审批通过率（0-1） */
    private BigDecimal amountThresholdPassedRate;

    /** PROCUREMENT_MANAGER 审批工作量 */
    private Integer procurementManagerWorkload;

    /** 委外成本占比 */
    private BigDecimal outsourceCostRatio;

    /** 趋势图数据（30 天逐日） */
    private List<TrendPoint> trendChart;

    @Data
    public static class TrendPoint implements Serializable {
        private LocalDate date;
        private Integer noOrderCount;
        private BigDecimal amount;
    }
}