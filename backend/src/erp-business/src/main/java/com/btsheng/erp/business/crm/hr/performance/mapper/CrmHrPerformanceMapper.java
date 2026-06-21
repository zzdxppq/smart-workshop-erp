package com.btsheng.erp.business.crm.hr.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrPerformance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmHrPerformanceMapper extends BaseMapper<CrmHrPerformance> {

    @Select("SELECT * FROM crm_hr_performance WHERE id = #{id} LIMIT 1")
    CrmHrPerformance selectById(@Param("id") Long id);

    @Select("SELECT * FROM crm_hr_performance " +
            "WHERE (#{employeeId} IS NULL OR employee_id = #{employeeId}) " +
            "AND (#{periodYear} IS NULL OR period_year = #{periodYear}) " +
            "AND (#{periodMonth} IS NULL OR period_month = #{periodMonth}) " +
            "ORDER BY score DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectPerformances(@Param("employeeId") Long employeeId,
                                                @Param("periodYear") Integer periodYear,
                                                @Param("periodMonth") Integer periodMonth,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);

    @Select("SELECT * FROM crm_hr_performance " +
            "WHERE employee_id = #{employeeId} " +
            "AND period_year = #{periodYear} AND period_month = #{periodMonth} LIMIT 1")
    CrmHrPerformance selectByEmployeeAndPeriod(@Param("employeeId") Long employeeId,
                                               @Param("periodYear") Integer periodYear,
                                               @Param("periodMonth") Integer periodMonth);
}
