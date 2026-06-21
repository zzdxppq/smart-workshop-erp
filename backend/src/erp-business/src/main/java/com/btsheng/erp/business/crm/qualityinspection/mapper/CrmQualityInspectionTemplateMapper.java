package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityInspectionTemplateMapper extends BaseMapper<CrmQualityInspectionTemplate> {

    @Select("""
            SELECT * FROM crm_quality_inspection_template
            WHERE status = #{status}
            ORDER BY updated_at DESC
            """)
    List<CrmQualityInspectionTemplate> selectByStatus(@Param("status") String status);

    @Select("""
            SELECT * FROM crm_quality_inspection_template
            WHERE status = 'ACTIVE'
              AND (inspection_type IS NULL OR inspection_type = '' OR inspection_type = #{inspectionType})
            ORDER BY
              CASE WHEN drawing_no_pattern IS NOT NULL AND drawing_no_pattern <> '' THEN 0 ELSE 1 END,
              updated_at DESC
            """)
    List<CrmQualityInspectionTemplate> selectActiveForType(@Param("inspectionType") String inspectionType);
}
