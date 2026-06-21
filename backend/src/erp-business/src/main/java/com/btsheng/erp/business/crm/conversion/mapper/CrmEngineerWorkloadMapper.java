package com.btsheng.erp.business.crm.conversion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmEngineerWorkload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * V1.3.7 · Story 1.8 · 工程师工作量 Mapper
 */
@Mapper
public interface CrmEngineerWorkloadMapper extends BaseMapper<CrmEngineerWorkload> {

    @Select("SELECT * FROM crm_engineer_workload WHERE user_id = #{userId} AND work_date = #{workDate} LIMIT 1")
    CrmEngineerWorkload selectByUserAndDate(Long userId, LocalDate workDate);

    @Select("SELECT * FROM crm_engineer_workload WHERE work_date = #{workDate} ORDER BY annotation_count + conversion_count DESC")
    List<CrmEngineerWorkload> selectByDate(LocalDate workDate);
}
