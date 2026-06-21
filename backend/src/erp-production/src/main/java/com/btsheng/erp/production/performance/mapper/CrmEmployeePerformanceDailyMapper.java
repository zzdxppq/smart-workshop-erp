package com.btsheng.erp.production.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.performance.entity.CrmEmployeePerformanceDaily;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CrmEmployeePerformanceDailyMapper extends BaseMapper<CrmEmployeePerformanceDaily> {

    @Delete("DELETE FROM crm_employee_performance_daily WHERE stat_date = #{statDate}")
    int deleteByStatDate(@Param("statDate") LocalDate statDate);

    @Select("""
            SELECT DATE(r.reported_at) AS statDate,
                   COALESCE(SUM(r.reported_qty), 0) AS finishedQty,
                   CASE WHEN COALESCE(SUM(r.reported_qty), 0) = 0 THEN 0
                        ELSE ROUND(SUM(CASE WHEN r.is_abnormal = 0 THEN r.reported_qty ELSE 0 END) / SUM(r.reported_qty), 4)
                   END AS passRate
            FROM crm_production_report r
            WHERE DATE(r.reported_at) BETWEEN #{from} AND #{to}
              AND (#{operatorId} IS NULL OR r.reported_by = #{operatorId})
            GROUP BY DATE(r.reported_at)
            ORDER BY statDate
            """)
    List<Map<String, Object>> trendFromDailyAgg(@Param("from") LocalDate from,
                                                 @Param("to") LocalDate to,
                                                 @Param("operatorId") Long operatorId);

    @Select("""
            SELECT stat_date AS statDate,
                   COALESCE(SUM(finished_qty), 0) AS finishedQty,
                   CASE WHEN COALESCE(SUM(finished_qty), 0) = 0 THEN 0
                        ELSE ROUND(SUM(qualified_qty) / SUM(finished_qty), 4)
                   END AS passRate
            FROM crm_employee_performance_daily
            WHERE stat_date BETWEEN #{from} AND #{to}
              AND operator_id IS NOT NULL
              AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
            GROUP BY stat_date
            ORDER BY stat_date
            """)
    List<Map<String, Object>> trendFromAggTable(@Param("from") LocalDate from,
                                                 @Param("to") LocalDate to,
                                                 @Param("operatorId") Long operatorId);
}
