package com.btsheng.erp.business.crm.gmsummary.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 Sprint 7 · Story 4.3 · 总经理汇总报表 Mapper（集成 D 实装）
 *
 * <p>聚合 SQL 全部基于现有 crm_purchase_order + sys_workflow_node + sys_role 表，
 * 无需新表。sys_workflow_event 表（V1.3.7 规划但未实装）留 Sprint 8 backlog。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Mapper
public interface GmSummaryMapper {

    /**
     * 6 项指标聚合（Story 4.3 AC-4.3.1）
     * <p>单 SQL 返回 Map（key-value 灵活应对部分指标为 0 的场景）
     */
    @Select("""
            SELECT
              COUNT(CASE WHEN source_type='NO_ORDER' THEN 1 END)                          AS no_order_count,
              COALESCE(SUM(CASE WHEN source_type='NO_ORDER' THEN total_amount ELSE 0 END),0) AS no_order_amount,
              COUNT(CASE WHEN purchase_reason='URGENT_REPLENISH' THEN 1 END)               AS urgent_count,
              -- 金额 &gt; 1 万审批通过率
              COALESCE(
                SUM(CASE WHEN approval_status='APPROVED' AND total_amount &gt; 10000 THEN 1 ELSE 0 END) /
                NULLIF(SUM(CASE WHEN total_amount &gt; 10000 THEN 1 ELSE 0 END), 0),
                0
              )                                                                           AS pass_rate
            FROM crm_purchase_order
            WHERE created_at >= #{startDate} AND created_at &lt; #{endDate}
            """)
    Map<String, Object> aggregateMetrics(@Param("startDate") LocalDateTime start,
                                         @Param("endDate") LocalDateTime end);

    /**
     * PROCUREMENT_MANAGER 工作量（Story 4.3 AC-4.3.1）
     * <p>V1.3.8 Sprint 8 Story 8.3：改用 sys_workflow_event 真实事件统计（替代集成 D 的 mock）
     */
    @Select("""
            SELECT COUNT(*) AS pm_workload
            FROM sys_workflow_event
            WHERE workflow_code = 'PO_APPROVAL'
              AND approver_role = 'PROCUREMENT_MANAGER'
              AND event_type = 'APPROVED'
              AND created_at >= #{startDate} AND created_at &lt; #{endDate}
            """)
    Integer countProcurementManagerWorkload(@Param("startDate") java.time.LocalDateTime start,
                                            @Param("endDate") java.time.LocalDateTime end);

    /**
     * 委外成本占比（Story 4.3 AC-4.3.1 · V1.3.8 Sprint 8 Story 8.6 实装）
     *
     * <p>Sprint 7 集成 D 用 mock（0.0），Sprint 8 Story 8.6 改为调用同模块的
     * OutsourceDashboardService.getCostRatio()（不走跨模块 Feign，因为委外面板
     * 也在 erp-business 模块，跨模块无收益）
     */
    @Select("""
            SELECT COALESCE(SUM(metric_value), 0) / NULLIF((SELECT COALESCE(SUM(total_amount), 0)
                                                            FROM crm_purchase_order
                                                            WHERE status IN ('PENDING_SHIP', 'PARTIAL_ARRIVED', 'ALL_ARRIVED')), 0) AS ratio
            FROM crm_outsource_dashboard
            WHERE metric_type = 'COST'
            """)
    Double selectOutsourceCostRatio();

    /**
     * trend_chart：30 天逐日趋势（Story 4.3 §4.1）
     */
    @Select("""
            SELECT
              DATE(created_at)                                       AS trend_date,
              COUNT(CASE WHEN source_type='NO_ORDER' THEN 1 END)     AS no_order_count,
              COALESCE(SUM(CASE WHEN source_type='NO_ORDER' THEN total_amount ELSE 0 END),0) AS amount
            FROM crm_purchase_order
            WHERE created_at >= #{startDate} AND created_at &lt; #{endDate}
            GROUP BY DATE(created_at)
            ORDER BY trend_date
            """)
    List<Map<String, Object>> trendChart(@Param("startDate") LocalDateTime start,
                                         @Param("endDate") LocalDateTime end);
}