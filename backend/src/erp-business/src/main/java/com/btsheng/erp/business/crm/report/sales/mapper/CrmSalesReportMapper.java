package com.btsheng.erp.business.crm.report.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.report.sales.entity.CrmSalesReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmSalesReportMapper extends BaseMapper<CrmSalesReport> {

    @Select("SELECT * FROM crm_sales_report " +
            "WHERE rank_no IS NOT NULL AND (#{period} IS NULL OR report_period = #{period}) " +
            "ORDER BY rank_no ASC LIMIT #{limit}")
    List<CrmSalesReport> selectRanking(@Param("period") String period, @Param("limit") int limit);

    @Select("SELECT report_period AS period, SUM(amount) AS totalAmount, SUM(order_count) AS totalOrders " +
            "FROM crm_sales_report WHERE rank_no IS NULL " +
            "AND report_period >= #{from} AND report_period <= #{to} " +
            "GROUP BY report_period ORDER BY report_period ASC")
    List<Map<String, Object>> selectTrend(@Param("from") String from, @Param("to") String to);

    @Select("SELECT customer_name AS customer, SUM(amount) AS totalAmount, SUM(order_count) AS totalOrders " +
            "FROM crm_sales_report WHERE rank_no IS NOT NULL " +
            "AND (#{period} IS NULL OR report_period = #{period}) " +
            "GROUP BY customer_name ORDER BY totalAmount DESC LIMIT #{limit}")
    List<Map<String, Object>> selectCustomerAnalysis(@Param("period") String period, @Param("limit") int limit);
}
