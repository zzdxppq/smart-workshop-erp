package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityInspectionItemMapper extends BaseMapper<CrmQualityInspectionItem> {

    @Select("SELECT * FROM crm_quality_inspection_item WHERE inspection_id = #{inspectionId}")
    List<CrmQualityInspectionItem> selectByInspectionId(@Param("inspectionId") Long inspectionId);

    @Select("SELECT COUNT(*) FROM crm_quality_inspection_item WHERE inspection_id = #{inspectionId} AND severity = 'CRITICAL'")
    int countCriticalByInspectionId(@Param("inspectionId") Long inspectionId);
}
