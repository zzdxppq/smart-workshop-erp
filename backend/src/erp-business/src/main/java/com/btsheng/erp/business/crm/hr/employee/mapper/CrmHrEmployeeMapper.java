package com.btsheng.erp.business.crm.hr.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmHrEmployeeMapper extends BaseMapper<CrmHrEmployee> {

    @Select("SELECT * FROM crm_hr_employee WHERE employee_no = #{employeeNo} LIMIT 1")
    CrmHrEmployee selectByEmployeeNo(@Param("employeeNo") String employeeNo);

    @Select("SELECT * FROM crm_hr_employee WHERE id = #{id} LIMIT 1")
    CrmHrEmployee selectById(@Param("id") Long id);

    @Select("SELECT * FROM crm_hr_employee WHERE user_id = #{userId} LIMIT 1")
    CrmHrEmployee selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM crm_hr_employee " +
            "WHERE (#{department} IS NULL OR department = #{department}) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "AND (#{position} IS NULL OR position = #{position}) " +
            "ORDER BY id ASC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectEmployees(@Param("department") String department,
                                              @Param("status") String status,
                                              @Param("position") String position,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Select("SELECT * FROM crm_hr_employee WHERE status = 'ACTIVE' ORDER BY id ASC")
    List<CrmHrEmployee> selectActiveEmployees();

    @Update("UPDATE crm_hr_employee SET on_leave = #{onLeave}, updated_at = NOW() WHERE id = #{id}")
    int updateOnLeave(@Param("id") Long id, @Param("onLeave") int onLeave);
}
