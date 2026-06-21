package com.btsheng.erp.business.finance.profit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.profit.entity.CrmProfitAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmProfitAnalysisMapper extends BaseMapper<CrmProfitAnalysis> {

    @Select("SELECT * FROM crm_profit_analysis WHERE order_id = #{orderId}")
    CrmProfitAnalysis selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM crm_profit_analysis ORDER BY settled_date DESC")
    List<CrmProfitAnalysis> selectAll();

    @Select("SELECT p.* FROM crm_profit_analysis p " +
            "INNER JOIN crm_order o ON o.id = p.order_id " +
            "WHERE (#{ownerUserId} IS NULL OR o.owner_user_id = #{ownerUserId}) " +
            "AND (#{deptId} IS NULL OR o.dept_id = #{deptId}) " +
            "ORDER BY p.settled_date DESC")
    List<CrmProfitAnalysis> selectAllScoped(@Param("ownerUserId") Long ownerUserId,
                                              @Param("deptId") Long deptId);

    @Select("SELECT p.* FROM crm_profit_analysis p " +
            "INNER JOIN crm_order o ON o.id = p.order_id " +
            "WHERE p.analysis_month = #{month} " +
            "AND (#{ownerUserId} IS NULL OR o.owner_user_id = #{ownerUserId}) " +
            "AND (#{deptId} IS NULL OR o.dept_id = #{deptId}) " +
            "ORDER BY p.profit DESC")
    List<CrmProfitAnalysis> selectByMonthScoped(@Param("month") String month,
                                                 @Param("ownerUserId") Long ownerUserId,
                                                 @Param("deptId") Long deptId);

    @Select("SELECT * FROM crm_profit_analysis WHERE alert_level IN ('WARNING', 'CRITICAL') ORDER BY profit_rate ASC LIMIT #{limit}")
    List<CrmProfitAnalysis> selectProfitAlerts(@Param("limit") int limit);

    @Select("SELECT * FROM crm_profit_analysis WHERE analysis_month = #{month} ORDER BY profit DESC")
    List<CrmProfitAnalysis> selectByMonth(@Param("month") String month);

    @Select("SELECT p.customer_id, p.customer_name, " +
            "SUM(p.profit) AS total_profit, " +
            "SUM(p.revenue) AS total_revenue, " +
            "COUNT(*) AS order_count " +
            "FROM crm_profit_analysis p " +
            "INNER JOIN crm_order o ON o.id = p.order_id " +
            "WHERE (#{ownerUserId} IS NULL OR o.owner_user_id = #{ownerUserId}) " +
            "AND (#{deptId} IS NULL OR o.dept_id = #{deptId}) " +
            "GROUP BY p.customer_id, p.customer_name " +
            "ORDER BY total_profit DESC")
    List<Map<String, Object>> selectCustomerRankingScoped(@Param("ownerUserId") Long ownerUserId,
                                                            @Param("deptId") Long deptId);

    @Select("SELECT p.analysis_month, " +
            "SUM(p.revenue) AS total_revenue, " +
            "SUM(p.total_cost) AS total_cost, " +
            "SUM(p.profit) AS total_profit, " +
            "AVG(p.profit_rate) AS avg_profit_rate " +
            "FROM crm_profit_analysis p " +
            "INNER JOIN crm_order o ON o.id = p.order_id " +
            "WHERE (#{ownerUserId} IS NULL OR o.owner_user_id = #{ownerUserId}) " +
            "AND (#{deptId} IS NULL OR o.dept_id = #{deptId}) " +
            "GROUP BY p.analysis_month " +
            "ORDER BY p.analysis_month ASC")
    List<Map<String, Object>> selectMonthlyTrendScoped(@Param("ownerUserId") Long ownerUserId,
                                                        @Param("deptId") Long deptId);
}
