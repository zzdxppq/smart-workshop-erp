package com.btsheng.erp.business.crm.hr.scheme.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface HrPerformanceAggMapper {

    @Select("""
            SELECT COALESCE(SUM(d.finished_qty), 0) AS finishedQty,
                   CASE WHEN COALESCE(SUM(d.finished_qty), 0) = 0 THEN 0
                        ELSE ROUND(SUM(d.qualified_qty) / SUM(d.finished_qty), 4)
                   END AS passRate,
                   CASE WHEN COALESCE(SUM(d.std_minutes), 0) = 0 THEN 0
                        ELSE ROUND(SUM(d.actual_minutes) / SUM(d.std_minutes), 4)
                   END AS utilizationRate
            FROM crm_employee_performance_daily d
            WHERE d.operator_id = #{operatorUserId}
              AND YEAR(d.stat_date) = #{year}
              AND MONTH(d.stat_date) = #{month}
            """)
    Map<String, Object> monthlyAggByOperator(@Param("operatorUserId") Long operatorUserId,
                                             @Param("year") int year,
                                             @Param("month") int month);

    @Select("""
            SELECT COUNT(DISTINCT DATE(a.clock_at)) AS attendanceDays
            FROM crm_hr_attendance a
            WHERE a.employee_id = #{employeeId}
              AND a.clock_type = 'IN'
              AND YEAR(a.clock_at) = #{year}
              AND MONTH(a.clock_at) = #{month}
            """)
    int attendanceDays(@Param("employeeId") Long employeeId,
                       @Param("year") int year,
                       @Param("month") int month);
}
