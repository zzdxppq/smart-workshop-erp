package com.btsheng.erp.business.crm.hr.scheme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrSalaryPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrmHrSalaryPackageMapper extends BaseMapper<CrmHrSalaryPackage> {

    @Select("SELECT * FROM crm_hr_salary_package WHERE is_default = 1 LIMIT 1")
    CrmHrSalaryPackage selectDefault();

    @Select("SELECT * FROM crm_hr_salary_package WHERE position = #{position} LIMIT 1")
    CrmHrSalaryPackage selectByPosition(String position);
}
