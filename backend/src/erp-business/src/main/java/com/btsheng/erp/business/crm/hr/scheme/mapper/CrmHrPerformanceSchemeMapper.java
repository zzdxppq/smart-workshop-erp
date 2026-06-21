package com.btsheng.erp.business.crm.hr.scheme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrPerformanceScheme;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrmHrPerformanceSchemeMapper extends BaseMapper<CrmHrPerformanceScheme> {

    @Select("SELECT * FROM crm_hr_performance_scheme WHERE is_default = 1 LIMIT 1")
    CrmHrPerformanceScheme selectDefault();

    @Select("SELECT * FROM crm_hr_performance_scheme WHERE position = #{position} LIMIT 1")
    CrmHrPerformanceScheme selectByPosition(String position);
}
