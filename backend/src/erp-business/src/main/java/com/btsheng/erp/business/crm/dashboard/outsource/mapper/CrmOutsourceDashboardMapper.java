package com.btsheng.erp.business.crm.dashboard.outsource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.dashboard.outsource.entity.CrmOutsourceDashboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmOutsourceDashboardMapper extends BaseMapper<CrmOutsourceDashboard> {

    @Select("SELECT dashboard_no, outsource_no, vendor_name, status, metric_value, quality_pass_rate, alert_level " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'OVERVIEW' " +
            "ORDER BY snapshot_at DESC LIMIT #{limit}")
    List<CrmOutsourceDashboard> selectOverview(@Param("limit") int limit);

    @Select("SELECT status, metric_value AS cnt " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'STATUS_DIST' " +
            "ORDER BY metric_value DESC")
    List<Map<String, Object>> selectStatusDistribution();

    @Select("SELECT dashboard_no, outsource_no, vendor_name, status, metric_name, metric_value, quality_pass_rate, alert_level " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'QUALITY' " +
            "AND (#{vendor} IS NULL OR vendor_name = #{vendor}) " +
            "ORDER BY quality_pass_rate ASC LIMIT #{limit}")
    List<CrmOutsourceDashboard> selectQuality(@Param("vendor") String vendor, @Param("limit") int limit);

    @Select("SELECT vendor_name, AVG(quality_pass_rate) AS avgPassRate, COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN alert_level IN ('WARN','HIGH') THEN 1 ELSE 0 END) AS alertCount " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'QUALITY' AND quality_pass_rate IS NOT NULL " +
            "GROUP BY vendor_name ORDER BY avgPassRate ASC")
    List<Map<String, Object>> selectQualityStats();

    @Select("SELECT dashboard_no, outsource_no, vendor_name, status, metric_name, metric_value, alert_level " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'COST' " +
            "ORDER BY metric_value DESC LIMIT #{limit}")
    List<CrmOutsourceDashboard> selectCost(@Param("limit") int limit);

    @Select("SELECT vendor_name, SUM(metric_value) AS totalCost, COUNT(*) AS orderCount " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'COST' " +
            "GROUP BY vendor_name ORDER BY totalCost DESC")
    List<Map<String, Object>> selectCostByVendor();

    // V1.3.8 Sprint 8 Story 8.6 · 委外成本聚合（gm:summary outsourceCostRatio）
            @Select("""
            SELECT COALESCE(SUM(metric_value), 0)
            FROM crm_outsource_dashboard
            WHERE metric_type = 'COST'
            """)
    java.math.BigDecimal selectOutsourceTotal();

    // V1.3.8 Sprint 8 Story 8.6 · 所有采购订单总金额（gm:summary outsourceCostRatio 分母）
            @Select("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM crm_purchase_order
            WHERE status IN ('PENDING_SHIP', 'PARTIAL_ARRIVED', 'ALL_ARRIVED')
            """)
    java.math.BigDecimal selectAllPoTotal();

    @Select("SELECT dashboard_no, outsource_no, vendor_name, status, metric_name, metric_value, alert_level " +
            "FROM crm_outsource_dashboard " +
            "WHERE metric_type = 'ALERT' " +
            "AND (#{level} IS NULL OR alert_level = #{level}) " +
            "ORDER BY CASE alert_level WHEN 'HIGH' THEN 1 WHEN 'WARN' THEN 2 ELSE 3 END, metric_value DESC")
    List<CrmOutsourceDashboard> selectAlerts(@Param("level") String level);
}
