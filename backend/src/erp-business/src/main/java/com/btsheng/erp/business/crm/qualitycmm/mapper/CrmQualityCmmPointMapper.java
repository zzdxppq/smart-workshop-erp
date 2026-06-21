package com.btsheng.erp.business.crm.qualitycmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmmPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityCmmPointMapper extends BaseMapper<CrmQualityCmmPoint> {

    @Select("SELECT * FROM crm_quality_cmm_point WHERE cmm_id = #{cmmId}")
    List<CrmQualityCmmPoint> selectByCmmId(@Param("cmmId") Long cmmId);
}
