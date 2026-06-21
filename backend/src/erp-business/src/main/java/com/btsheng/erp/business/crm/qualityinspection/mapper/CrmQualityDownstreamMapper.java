package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityDownstream;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityDownstreamMapper extends BaseMapper<CrmQualityDownstream> {

    @Select("SELECT * FROM crm_quality_downstream WHERE inspection_id = #{inspectionId} ORDER BY created_at DESC")
    List<CrmQualityDownstream> selectByInspectionId(@Param("inspectionId") Long inspectionId);
}
