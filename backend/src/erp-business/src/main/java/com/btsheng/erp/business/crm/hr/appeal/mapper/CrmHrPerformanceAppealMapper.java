package com.btsheng.erp.business.crm.hr.appeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.appeal.entity.CrmHrPerformanceAppeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmHrPerformanceAppealMapper extends BaseMapper<CrmHrPerformanceAppeal> {

    @Select("SELECT * FROM crm_hr_performance_appeal WHERE id = #{id} LIMIT 1")
    CrmHrPerformanceAppeal selectById(@Param("id") Long id);

    @Select("SELECT * FROM crm_hr_performance_appeal " +
            "WHERE (#{employeeId} IS NULL OR employee_id = #{employeeId}) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectAppeals(@Param("employeeId") Long employeeId,
                                            @Param("status") String status,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
}
