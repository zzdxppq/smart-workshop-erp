package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualitySample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualitySampleMapper extends BaseMapper<CrmQualitySample> {

    @Select("SELECT * FROM crm_quality_sample WHERE inspection_id = #{inspectionId}")
    List<CrmQualitySample> selectByInspectionId(@Param("inspectionId") Long inspectionId);
}
