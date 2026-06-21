package com.btsheng.erp.business.crm.hr.payroll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.payroll.entity.CrmHrPayroll;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmHrPayrollMapper extends BaseMapper<CrmHrPayroll> {

    @Select("SELECT * FROM crm_hr_payroll WHERE payroll_no = #{payrollNo} LIMIT 1")
    CrmHrPayroll selectByPayrollNo(@Param("payrollNo") String payrollNo);

    @Select("SELECT * FROM crm_hr_payroll WHERE id = #{id} LIMIT 1")
    CrmHrPayroll selectById(@Param("id") Long id);

    @Select("SELECT * FROM crm_hr_payroll " +
            "WHERE employee_id = #{employeeId} " +
            "AND period_year = #{periodYear} AND period_month = #{periodMonth} LIMIT 1")
    CrmHrPayroll selectByEmployeeAndPeriod(@Param("employeeId") Long employeeId,
                                          @Param("periodYear") Integer periodYear,
                                          @Param("periodMonth") Integer periodMonth);

    @Select("SELECT * FROM crm_hr_payroll " +
            "WHERE (#{employeeId} IS NULL OR employee_id = #{employeeId}) " +
            "AND (#{periodYear} IS NULL OR period_year = #{periodYear}) " +
            "AND (#{periodMonth} IS NULL OR period_month = #{periodMonth}) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectPayrolls(@Param("employeeId") Long employeeId,
                                            @Param("periodYear") Integer periodYear,
                                            @Param("periodMonth") Integer periodMonth,
                                            @Param("status") String status,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
}
